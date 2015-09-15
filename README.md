
# App Permissions


## Fragment

FragmentからでもrequestPermissionsできる。

FragmentにもonRequestPermissionsResultあるから、Overrideしてそこで受け取る

もはやPermissionの要求は、Fragment使ってmoduleみたいにした方がいいアプリもあるかもなー。

設計次第か。Activityに書くのはわりと煩雑になりそうな気がする。

checkSelfPermissionとかはContextじゃないとダメっぽい。

```java
java.lang.Exception
	at com.os.operando.m_preview_sample.PermissionRequestFragment.onRequestPermissionsResult(PermissionRequestFragment.java:30)
	at android.app.Activity.dispatchRequestPermissionsResultToFragment(Activity.java:6440)
	at android.app.Activity.dispatchActivityResult(Activity.java:6327)
	at android.app.ActivityThread.deliverResults(ActivityThread.java:3677)
	at android.app.ActivityThread.handleSendResult(ActivityThread.java:3724)
	at android.app.ActivityThread.-wrap16(ActivityThread.java)
	at android.app.ActivityThread$H.handleMessage(ActivityThread.java:1392)
	at android.os.Handler.dispatchMessage(Handler.java:102)
	at android.os.Looper.loop(Looper.java:148)
	at android.app.ActivityThread.main(ActivityThread.java:5401)
	at java.lang.reflect.Method.invoke(Native Method)
	at com.android.internal.os.ZygoteInit$MethodAndArgsCaller.run(ZygoteInit.java:725)
	at com.android.internal.os.ZygoteInit.main(ZygoteInit.java:615)
```

## Activity

```java
java.lang.Exception
	at com.os.operando.m_preview_sample.MainActivity.onRequestPermissionsResult(MainActivity.java:35)
	at android.app.Activity.dispatchRequestPermissionsResult(Activity.java:6431)
	at android.app.Activity.dispatchActivityResult(Activity.java:6308)
	at android.app.ActivityThread.deliverResults(ActivityThread.java:3677)
	at android.app.ActivityThread.handleSendResult(ActivityThread.java:3724)
	at android.app.ActivityThread.-wrap16(ActivityThread.java)
	at android.app.ActivityThread$H.handleMessage(ActivityThread.java:1392)
	at android.os.Handler.dispatchMessage(Handler.java:102)
	at android.os.Looper.loop(Looper.java:148)
	at android.app.ActivityThread.main(ActivityThread.java:5401)
	at java.lang.reflect.Method.invoke(Native Method)
	at com.android.internal.os.ZygoteInit$MethodAndArgsCaller.run(ZygoteInit.java:725)
	at com.android.internal.os.ZygoteInit.main(ZygoteInit.java:615)
```

## uses-permission-sdkに書かないでPermission Requestしたらどうなるのか??

Permission RequestのDialogが出なかった。

否定されたことになって返ってくる。

例外とかにはならない。


## 保存先周り

```
/data/system/users/{userId}/runtime-permissions.xml
```

/data/system/users/{userId}配下には、各ユーザの設定ファイル等がある。

Settingsのファイルとかウィジットの位置とか色々。

そこにPermissionのファイルもある = マルチユーザではユーザごとにPermissionの設定を持つ

権限は以下のようになってるから、通常のアプリ等から読み取れない。
Systemプロセス関連だけ。

```
-rw------- system   system      12227 2015-07-08 00:17 runtime-permissions.xml
```

暗号化ファイル システム (EFS)

### flagsって何??

http://tools.oesf.biz/android-MNC/xref/com/android/server/pm/PermissionsState.java#PERMISSION_OPERATION_FAILURE

ココらへんの定数関係あるかも

Permissionを要求する必要のないアプリのパッケージ名は記載されないっぽい

書き込まれるタイミングは不明

```
<pkg name="com.os.operando.m_preview_sample">
  <item name="android.permission.READ_PHONE_STATE" granted="true" flags="0" />
</pkg>
```


## Stting画面のPermission

```
com.android.packageinstaller/.permission.ui.ManagePermissionsActivity
```

```
Active Fragments in e200fdf:
  #0: AppPermissionsFragment{df6e42c #0 id=0x1020002}
    mFragmentId=#1020002 mContainerId=#1020002 mTag=null
    mState=5 mIndex=0 mWho=android:fragment:0 mBackStackNesting=0
    mAdded=true mRemoving=false mResumed=true mFromLayout=false mInLayout=false
    mHidden=false mDetached=false mMenuVisible=true mHasMenu=true
    mRetainInstance=false mRetaining=false mUserVisibleHint=true
    mFragmentManager=FragmentManager{e200fdf in HostCallbacks{1acbef5}}
    mHost=android.app.Activity$HostCallbacks@1acbef5
    mArguments=Bundle[{android.intent.extra.PACKAGE_NAME=com.os.operando.m_preview_sample}] // これ興味深い
    mContainer=android.widget.FrameLayout{f3a248a V.E...... ........ 0,320-1440,2368 #1020002 android:id/content}
    mView=android.widget.FrameLayout{f9fcafb V.E...... ........ 0,0-1440,2048 #7f0b0099 app:id/prefs_container}
    Child FragmentManager{41718 in AppPermissionsFragment{df6e42c}}:
      FragmentManager misc state:
        mHost=android.app.Activity$HostCallbacks@1acbef5
        mContainer=android.app.Fragment$1@5575571
        mParent=AppPermissionsFragment{df6e42c #0 id=0x1020002}
        mCurState=5 mStateSaved=false mDestroyed=false
```

```
I/ActivityManager( 1336): START u0 {act=android.intent.action.MANAGE_APP_PERMISSIONS cmp=com.android.packageinstaller/.permission.ui.ManagePermissionsActivity (has extras)} from uid 1000 on display 0
V/WindowManager( 1336): addAppToken: AppWindowToken{fa8b3d3 token=Token{49449c2 ActivityRecord{3dfd60d u0 com.android.packageinstaller/.permission.ui.ManagePermissionsActivity t43}}} to stack=6 task=43 at 3
V/WindowManager( 1336): Adding window Window{dc0c609 u0 com.android.packageinstaller/com.android.packageinstaller.permission.ui.ManagePermissionsActivity} at 6 of 10 (after Window{da79774 u0 com.android.settings/com.android.settings.SubSettings})
I/ActivityManager( 1336): Displayed com.android.packageinstaller/.permission.ui.ManagePermissionsActivity: +800ms
```

### adb shell am start -a android.intent.action.MANAGE_APP_PERMISSIONSの結果

```
E/StrictMode( 2510): class com.android.packageinstaller.permission.ui.ManagePermissionsActivity; instances=3; limit=1
E/StrictMode( 2510): android.os.StrictMode$InstanceCountViolation: class com.android.packageinstaller.permission.ui.ManagePermissionsActivity; instances=3; limit=1
E/StrictMode( 2510): 	at android.os.StrictMode.setClassInstanceLimit(StrictMode.java:1)
```

### log

```
I snet_event_log: [permissions_requested,10059,com.os.operando.m_preview_sample:android.permission-group.PHONE|true|0]

I snet_event_log: [permissions_toggled,10042,com.os.operando.m_preview_sample:android.permission-group.PHONE|false|1]
```

### Other

```
/system/app/PackageInstaller/PackageInstaller.apk
```

```
<activity android:configChanges="keyboardHidden|orientation|screenSize" android:excludeFromRecents="true" android:label="@string/app_permissions" android:name=".permission.ui.ManagePermissionsActivity" android:permission="android.permission.GRANT_REVOKE_PERMISSIONS" android:theme="@style/Settings">
    <intent-filter>
        <action android:name="android.intent.action.MANAGE_PERMISSIONS"/>
        <action android:name="android.intent.action.MANAGE_APP_PERMISSIONS"/>
        <action android:name="android.intent.action.MANAGE_PERMISSION_APPS"/>
        <category android:name="android.intent.category.DEFAULT"/>
    </intent-filter>
</activity>
```

#### adb shell dumpsys package

```
Package [com.os.operando.m_preview_sample] (d7ac27b):
  userId=10059
  pkg=Package{bc4aa8e com.os.operando.m_preview_sample}
  codePath=/data/app/com.os.operando.m_preview_sample-2
  resourcePath=/data/app/com.os.operando.m_preview_sample-2
  legacyNativeLibraryDir=/data/app/com.os.operando.m_preview_sample-2/lib
  primaryCpuAbi=null
  secondaryCpuAbi=null
  versionCode=1 targetSdk=10000
  versionName=1.0
  splits=[base]
  applicationInfo=ApplicationInfo{68812af com.os.operando.m_preview_sample}
  flags=[ DEBUGGABLE HAS_CODE ALLOW_CLEAR_USER_DATA ALLOW_BACKUP ]
  priavateFlags=[ ]
  dataDir=/data/user/0/com.os.operando.m_preview_sample
  supportsScreens=[small, medium, large, xlarge, resizeable, anyDensity]
  timeStamp=2015-07-08 01:40:47
  firstInstallTime=2015-06-20 17:08:39
  lastUpdateTime=2015-07-08 01:40:47
  signatures=PackageSignatures{bb0fcd6 [ffb76bc]}
  installPermissionsFixed=true installStatus=1
  pkgFlags=[ DEBUGGABLE HAS_CODE ALLOW_CLEAR_USER_DATA ALLOW_BACKUP ]
  User 0:  installed=true hidden=false stopped=false notLaunched=false enabled=0
    runtime permissions:
      android.permission.READ_PHONE_STATE, granted=false, flags=0x1
      android.permission.CAMERA, granted=false, flags=0x1



      Package [com.android.launcher] (ae3a6a):
        userId=10008
        pkg=Package{124f15c com.android.launcher}
        codePath=/system/priv-app/Launcher2
        resourcePath=/system/priv-app/Launcher2
        legacyNativeLibraryDir=/system/priv-app/Launcher2/lib
        primaryCpuAbi=null
        secondaryCpuAbi=null
        versionCode=22 targetSdk=10000
        versionName=M-1955487
        splits=[base]
        applicationInfo=ApplicationInfo{67931ef com.android.launcher}
        flags=[ SYSTEM HAS_CODE ALLOW_CLEAR_USER_DATA ALLOW_BACKUP ]
        priavateFlags=[ PRIVILEGED ]
        dataDir=/data/user/0/com.android.launcher
        supportsScreens=[small, medium, large, xlarge, resizeable, anyDensity]
        timeStamp=2015-05-25 16:08:44
        firstInstallTime=2015-05-25 16:08:44
        lastUpdateTime=2015-05-25 16:08:44
        signatures=PackageSignatures{76cfc65 [96c6d3a]}
        installPermissionsFixed=true installStatus=1
        pkgFlags=[ SYSTEM HAS_CODE ALLOW_CLEAR_USER_DATA ALLOW_BACKUP ]
        install permissions:
          com.android.launcher.permission.WRITE_SETTINGS, granted=true, flags=0x0
          android.permission.BIND_APPWIDGET, granted=true, flags=0x0
          com.android.launcher.permission.READ_SETTINGS, granted=true, flags=0x0
          android.permission.SET_WALLPAPER, granted=true, flags=0x0
          android.permission.SET_WALLPAPER_HINTS, granted=true, flags=0x0
          android.permission.GET_ACCOUNTS, granted=true, flags=0x0
          android.permission.VIBRATE, granted=true, flags=0x0
        User 0:  installed=true hidden=false stopped=false notLaunched=false enabled=0
          runtime permissions:
            android.permission.CALL_PHONE, granted=true, flags=0x0
```

## 関連クラス


http://tools.oesf.biz/android-MNC/xref/com/android/server/pm/Settings.java
 Inner Class : RuntimePermissionPersistence

http://tools.oesf.biz/android-MNC/xref/com/android/server/pm/PermissionsState.java


## Sample Code

https://github.com/googlesamples/android-RuntimePermissions



## Permission

https://www.youtube.com/watch?v=f17qe9vZ8RM

http://developer.android.com/preview/features/runtime-permissions.html

```java
if (checkSelfPermission(Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
	requestPermissions(new String[]{Manifest.permission.READ_PHONE_STATE,}, MY_PERMISSIONS_REQUEST_READ_PHONE_STATE);
} else {
	showLineNumber();
}
```

新しいPermissionを追加してもDialog出ないで自動更新されるようになるっぽい

### Runtime Permissions

実行時にPermission要求を行う


### System Settings

設定からアプリのPermissionをユーザが随時コントロールできる

特定のPermissionを使っているアプリの一覧とかも出るようになる


IntentでCameraとか呼び出す場合はPermission要求不要??


### Request Multiple Permissions

一度で複数のPermission要求が行える

1 of 3 とかDialogに表示される


PermissionをRequestしたら、すぐにそのRequestしたものに対してフィードバックしてやれ

じゃないと、なんで聞いてきたのかユーザはわからなくて、Permission OFFにしに行くかもしれんからな


checkSelfPermission

requestPermissions


Permissionを要求するAPIに対して、新しいASでは注釈が出る様になってる

Lintもある


アプリケーションがM向けに作られたものではない場合、今と同じようにインストール時にPermissionを要求する?

こちらも同様に設定アプリケーションから個別にパーミッションを剥奪することが可能となっている.

システムはその場合に”アプリケーションが正常に動作しない可能性がある”旨のダイアログを表示してパーミッションの剥奪を行う.

レガシーなアプリケーションがパーミッションを剥奪された場合, パーミッションが必要なAPIを実行すると必ずセキュリティーパーミッションが発生するとは限らない.

代わりに空のデータが返却されることもあるだろうし, エラーを意味するコードが返されるかもしれないし, 予期しない動作となる場合もある.

例えば, カレンダー情報の検索に必要なクエリをパーミッションの使用許可なしで実行した場合, クエリの結果は”空”を返す.


データ削除 and アンインストールしたらPermission要求の情報はどうなるのか？



```bash
$ adb shell pm grant <package_name> <permission_name>

$ adb shell pm revoke <package_name> <permission_name>
```

```
AppOps  ( 1323): Bad call: specified package com.os.operando.m_preview_sample under uid 10042 but it is really 10059

com.android.packageinstaller/com.android.packageinstaller.permission.ui.ManagePermissionsActivity

android.content.pm.action.REQUEST_PERMISSIONS pkg=com.android.packageinstaller cmp=com.android.packageinstaller/.permission.ui.GrantPermissionsActivity

com.android.packageinstaller.permission.ui.ManualLayoutFrame
```


## 許可をもらってるPermissionともらってないPermissionでRequestしたらどうなるのか？？

許可をもらってないPermissionの要求だけ行われる

結果は許可をもらってるPermission + もらってないPermissionのStatusが返ってくる

Explain why you need permissions


## アプリのPermission画面に飛ぶIntentはないのか？？

画面

com.android.packageinstaller/.permission.ui.ManagePermissionsActivity + AppPermissionsFragment


## Souce

### pm revoke

http://tools.oesf.biz/android-5.0.0_r2.0/xref/frameworks/base/cmds/pm/src/com/android/commands/pm/Pm.java#runGrantRevokePermission
  http://tools.oesf.biz/android-5.0.0_r2.0/xref/frameworks/base/core/java/android/app/ApplicationPackageManager.java#435
    http://tools.oesf.biz/android-5.0.0_r2.0/xref/frameworks/base/services/core/java/com/android/server/pm/PackageManagerService.java#2643


http://tools.oesf.biz/android-5.0.0_r2.0/xref/frameworks/base/services/core/java/com/android/server/pm/GrantedPermissions.java#GrantedPermissions



## Memo


mArguments=Bundle[{android.intent.extra.PACKAGE_NAME=com.kouzoh.mercari}]

```bash
adb shell am start -a android.intent.action.MANAGE_APP_PERMISSIONS -e android.intent.extra.PACKAGE_NAME com.kouzoh.mercari
```

```java
Intent i = new Intent();
i.setAction("android.intent.action.MANAGE_APP_PERMISSIONS");
i.putExtra("android.intent.extra.PACKAGE_NAME", "com.kouzoh.mercari");
startActivity(i);
```

```java
// Intent.java - SKD 23

/**
 * Intent extra: An app package name.
 * <p>
 * Type: String
 * </p>
 *
 * @hide
 */
@SystemApi
public static final String EXTRA_PACKAGE_NAME = "android.intent.extra.PACKAGE_NAME";


/**
 * Activity action: Launch UI to manage the permissions of an app.
 * <p>
 * Input: {@link #EXTRA_PACKAGE_NAME} specifies the package whose permissions
 * will be managed by the launched UI.
 * </p>
 * <p>
 * Output: Nothing.
 * </p>
 *
 * @see #EXTRA_PACKAGE_NAME
 *
 * @hide
 */
@SdkConstant(SdkConstantType.ACTIVITY_INTENT_ACTION)
public static final String ACTION_MANAGE_APP_PERMISSIONS =
        "android.intent.action.MANAGE_APP_PERMISSIONS";

/**
  * Activity action: Launch UI to manage permissions.
  * <p>
  * Input: Nothing.
  * </p>
  * <p>
  * Output: Nothing.
  * </p>
  *
  * @hide
  */
 @SdkConstant(SdkConstantType.ACTIVITY_INTENT_ACTION)
 public static final String ACTION_MANAGE_PERMISSIONS =
         "android.intent.action.MANAGE_PERMISSIONS";
```
