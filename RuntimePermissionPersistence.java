private final class RuntimePermissionPersistence {
    private static final long WRITE_PERMISSIONS_DELAY_MILLIS = 200;

    private static final long MAX_WRITE_PERMISSIONS_DELAY_MILLIS = 2000;

    private final Handler mHandler = new MyHandler();

    private final Object mLock;

    @GuardedBy("mLock")
    private SparseBooleanArray mWriteScheduled = new SparseBooleanArray();

    @GuardedBy("mLock")
    private SparseLongArray mLastNotWrittenMutationTimesMillis = new SparseLongArray();

    public RuntimePermissionPersistence(Object lock) {
        mLock = lock;
    }

    public void writePermissionsForUserSyncLPr(int userId) {
        mHandler.removeMessages(userId);
        writePermissionsSync(userId);
    }

    public void writePermissionsForUserAsyncLPr(int userId) {
        final long currentTimeMillis = SystemClock.uptimeMillis();

        if (mWriteScheduled.get(userId)) {
            mHandler.removeMessages(userId);

            // If enough time passed, write without holding off anymore.
            final long lastNotWrittenMutationTimeMillis = mLastNotWrittenMutationTimesMillis
                    .get(userId);
            final long timeSinceLastNotWrittenMutationMillis = currentTimeMillis
                    - lastNotWrittenMutationTimeMillis;
            if (timeSinceLastNotWrittenMutationMillis >= MAX_WRITE_PERMISSIONS_DELAY_MILLIS) {
                mHandler.obtainMessage(userId).sendToTarget();
                return;
            }

            // Hold off a bit more as settings are frequently changing.
            final long maxDelayMillis = Math.max(lastNotWrittenMutationTimeMillis
                    + MAX_WRITE_PERMISSIONS_DELAY_MILLIS - currentTimeMillis, 0);
            final long writeDelayMillis = Math.min(WRITE_PERMISSIONS_DELAY_MILLIS,
                    maxDelayMillis);

            Message message = mHandler.obtainMessage(userId);
            mHandler.sendMessageDelayed(message, writeDelayMillis);
        } else {
            mLastNotWrittenMutationTimesMillis.put(userId, currentTimeMillis);
            Message message = mHandler.obtainMessage(userId);
            mHandler.sendMessageDelayed(message, WRITE_PERMISSIONS_DELAY_MILLIS);
            mWriteScheduled.put(userId, true);
        }
    }

    private void writePermissionsSync(int userId) {
        AtomicFile destination = new AtomicFile(getUserRuntimePermissionsFile(userId));

        ArrayMap<String, List<PermissionState>> permissionsForPackage = new ArrayMap<>();
        ArrayMap<String, List<PermissionState>> permissionsForSharedUser = new ArrayMap<>();

        synchronized (mLock) {
            mWriteScheduled.delete(userId);

            final int packageCount = mPackages.size();
            for (int i = 0; i < packageCount; i++) {
                String packageName = mPackages.keyAt(i);
                PackageSetting packageSetting = mPackages.valueAt(i);
                if (packageSetting.sharedUser == null) {
                    PermissionsState permissionsState = packageSetting.getPermissionsState();
                    List<PermissionState> permissionsStates = permissionsState
                            .getRuntimePermissionStates(userId);
                    if (!permissionsStates.isEmpty()) {
                        permissionsForPackage.put(packageName, permissionsStates);
                    }
                }
            }

            final int sharedUserCount = mSharedUsers.size();
            for (int i = 0; i < sharedUserCount; i++) {
                String sharedUserName = mSharedUsers.keyAt(i);
                SharedUserSetting sharedUser = mSharedUsers.valueAt(i);
                PermissionsState permissionsState = sharedUser.getPermissionsState();
                List<PermissionState> permissionsStates = permissionsState
                        .getRuntimePermissionStates(userId);
                if (!permissionsStates.isEmpty()) {
                    permissionsForSharedUser.put(sharedUserName, permissionsStates);
                }
            }
        }

        FileOutputStream out = null;
        try {
            out = destination.startWrite();

            XmlSerializer serializer = Xml.newSerializer();
            serializer.setOutput(out, "utf-8");
            serializer.setFeature(
                    "http://xmlpull.org/v1/doc/features.html#indent-output", true);
            serializer.startDocument(null, true);
            serializer.startTag(null, TAG_RUNTIME_PERMISSIONS);

            final int packageCount = permissionsForPackage.size();
            for (int i = 0; i < packageCount; i++) {
                String packageName = permissionsForPackage.keyAt(i);
                List<PermissionState> permissionStates = permissionsForPackage.valueAt(i);
                serializer.startTag(null, TAG_PACKAGE);
                serializer.attribute(null, ATTR_NAME, packageName);
                writePermissions(serializer, permissionStates);
                serializer.endTag(null, TAG_PACKAGE);
            }

            final int sharedUserCount = permissionsForSharedUser.size();
            for (int i = 0; i < sharedUserCount; i++) {
                String packageName = permissionsForSharedUser.keyAt(i);
                List<PermissionState> permissionStates = permissionsForSharedUser.valueAt(i);
                serializer.startTag(null, TAG_SHARED_USER);
                serializer.attribute(null, ATTR_NAME, packageName);
                writePermissions(serializer, permissionStates);
                serializer.endTag(null, TAG_SHARED_USER);
            }

            serializer.endTag(null, TAG_RUNTIME_PERMISSIONS);
            serializer.endDocument();
            destination.finishWrite(out);

            // Any error while writing is fatal.
        } catch (Throwable t) {
            Slog.wtf(PackageManagerService.TAG,
                    "Failed to write settings, restoring backup", t);
            destination.failWrite(out);
            throw new IllegalStateException("Failed to write runtime permissions,"
                    + " restoring backup", t);
        } finally {
            IoUtils.closeQuietly(out);
        }
    }

    private void onUserRemoved(int userId) {
        // Make sure we do not
        mHandler.removeMessages(userId);

        for (SettingBase sb : mPackages.values()) {
            revokeRuntimePermissionsAndClearFlags(sb, userId);
        }

        for (SettingBase sb : mSharedUsers.values()) {
            revokeRuntimePermissionsAndClearFlags(sb, userId);
        }
    }

    private void revokeRuntimePermissionsAndClearFlags(SettingBase sb, int userId) {
        PermissionsState permissionsState = sb.getPermissionsState();
        for (PermissionState permissionState
                : permissionsState.getRuntimePermissionStates(userId)) {
            BasePermission bp = mPermissions.get(permissionState.getName());
            if (bp != null) {
                permissionsState.revokeRuntimePermission(bp, userId);
                permissionsState.updatePermissionFlags(bp, userId,
                        PackageManager.MASK_PERMISSION_FLAGS, 0);
            }
        }
    }

    public void readStateForUserSyncLPr(int userId) {
        File permissionsFile = getUserRuntimePermissionsFile(userId);
        if (!permissionsFile.exists()) {
            return;
        }

        FileInputStream in;
        try {
            in = new FileInputStream(permissionsFile);
        } catch (FileNotFoundException fnfe) {
            Slog.i(PackageManagerService.TAG, "No permissions state");
            return;
        }

        try {
            XmlPullParser parser = Xml.newPullParser();
            parser.setInput(in, null);
            parseRuntimePermissionsLPr(parser, userId);

        } catch (XmlPullParserException | IOException e) {
            throw new IllegalStateException("Failed parsing permissions file: "
                    + permissionsFile , e);
        } finally {
            IoUtils.closeQuietly(in);
        }
    }

    private void parseRuntimePermissionsLPr(XmlPullParser parser, int userId)
            throws IOException, XmlPullParserException {
        final int outerDepth = parser.getDepth();
        int type;
        while ((type = parser.next()) != XmlPullParser.END_DOCUMENT
                && (type != XmlPullParser.END_TAG || parser.getDepth() > outerDepth)) {
            if (type == XmlPullParser.END_TAG || type == XmlPullParser.TEXT) {
                continue;
            }

            switch (parser.getName()) {
                case TAG_PACKAGE: {
                    String name = parser.getAttributeValue(null, ATTR_NAME);
                    PackageSetting ps = mPackages.get(name);
                    if (ps == null) {
                        Slog.w(PackageManagerService.TAG, "Unknown package:" + name);
                        XmlUtils.skipCurrentTag(parser);
                        continue;
                    }
                    parsePermissionsLPr(parser, ps.getPermissionsState(), userId);
                } break;

                case TAG_SHARED_USER: {
                    String name = parser.getAttributeValue(null, ATTR_NAME);
                    SharedUserSetting sus = mSharedUsers.get(name);
                    if (sus == null) {
                        Slog.w(PackageManagerService.TAG, "Unknown shared user:" + name);
                        XmlUtils.skipCurrentTag(parser);
                        continue;
                    }
                    parsePermissionsLPr(parser, sus.getPermissionsState(), userId);
                } break;
            }
        }
    }

    private void parsePermissionsLPr(XmlPullParser parser, PermissionsState permissionsState,
            int userId) throws IOException, XmlPullParserException {
        final int outerDepth = parser.getDepth();
        int type;
        while ((type = parser.next()) != XmlPullParser.END_DOCUMENT
                && (type != XmlPullParser.END_TAG || parser.getDepth() > outerDepth)) {
            if (type == XmlPullParser.END_TAG || type == XmlPullParser.TEXT) {
                continue;
            }

            switch (parser.getName()) {
                case TAG_ITEM: {
                    String name = parser.getAttributeValue(null, ATTR_NAME);
                    BasePermission bp = mPermissions.get(name);
                    if (bp == null) {
                        Slog.w(PackageManagerService.TAG, "Unknown permission:" + name);
                        XmlUtils.skipCurrentTag(parser);
                        continue;
                    }

                    String grantedStr = parser.getAttributeValue(null, ATTR_GRANTED);
                    final boolean granted = grantedStr == null
                            || Boolean.parseBoolean(grantedStr);

                    String flagsStr = parser.getAttributeValue(null, ATTR_FLAGS);
                    final int flags = (flagsStr != null)
                            ? Integer.parseInt(flagsStr, 16) : 0;

                    if (granted) {
                        if (permissionsState.grantRuntimePermission(bp, userId) ==
                                PermissionsState.PERMISSION_OPERATION_FAILURE) {
                            Slog.w(PackageManagerService.TAG, "Duplicate permission:" + name);
                        } else {
                            permissionsState.updatePermissionFlags(bp, userId,
                                    PackageManager.MASK_PERMISSION_FLAGS, flags);

                        }
                    } else {
                        if (permissionsState.revokeRuntimePermission(bp, userId) ==
                                PermissionsState.PERMISSION_OPERATION_FAILURE) {
                            Slog.w(PackageManagerService.TAG, "Duplicate permission:" + name);
                        } else {
                            permissionsState.updatePermissionFlags(bp, userId,
                                    PackageManager.MASK_PERMISSION_FLAGS, flags);
                        }
                    }

                } break;
            }
        }
    }

    private void writePermissions(XmlSerializer serializer,
            List<PermissionState> permissionStates) throws IOException {
        for (PermissionState permissionState : permissionStates) {
            serializer.startTag(null, TAG_ITEM);
            serializer.attribute(null, ATTR_NAME,permissionState.getName());
            serializer.attribute(null, ATTR_GRANTED,
                    String.valueOf(permissionState.isGranted()));
            serializer.attribute(null, ATTR_FLAGS,
                    Integer.toHexString(permissionState.getFlags()));
            serializer.endTag(null, TAG_ITEM);
        }
    }

    private final class MyHandler extends Handler {
        public MyHandler() {
            super(BackgroundThread.getHandler().getLooper());
        }

        @Override
        public void handleMessage(Message message) {
            final int userId = message.what;
            Runnable callback = (Runnable) message.obj;
            writePermissionsSync(userId);
            if (callback != null) {
                callback.run();
            }
        }
    }
}
