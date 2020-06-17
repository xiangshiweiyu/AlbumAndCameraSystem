#### 前言
好久好久，我都没有在博客写过东西了，最近还把之前写的还算是有价值的文章转移到了公众号上面，公众号，可以相对及时的回复各位朋友的问题，简书对了还有 csdn 只有主动关注才会发现各位的留言，所以，推荐各位小伙伴关注我的公众号哈，我的公众号是![欢迎关注 码虫韩小呆](https://upload-images.jianshu.io/upload_images/6433394-69832c80bdc58b5d.jpg?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)
####  一、适配背景
##### 1、Android 4.4及以上设备
 Android 4.4（简称 4.4）及以上设备 的图片文件路径与4.4以下设备的路径是完全不一样的，需要开发者自行拼接。
##### 2、Android 6.0 及以上设备
Android 6.0（简称 6.0 ）及以上设备 在调用一些功能的时候，开发者系统申请权限，部分权限属于危险权限，涉及到用户隐私相关问题，现在应用市场都强制要求进行相关适配了，如果开发者还未适配，那么只能证明您的软件，用户量好像很低了，建议开发者进行跳槽吧。
##### 3、Android 8.0 及以上设备
Android 8.0（简称8.0）及以上设备 在调用系统相机进行拍照的时候 需要在 `AndroidManifest.xml` 进行配置 ` provider` 将照片存储的位置 进行共享，否则用户拍摄之后的照片，就只会静静的躺在相册里，对了，配置，可能还会造成崩溃。
##### 4、Android 10及以上设备
在做文件的操作时都会申请存储空间的读写权限。但是这些权限完全被滥用，造成的问题就是手机的存储空间中充斥着大量不明作用的文件，并且应用卸载后它也没有删除掉。为了解决这个问题，Android 10 中引入了Scoped Storage 的概念，通过添加外部存储访问限制来实现更好的文件管理。
这里有一个问题是，什么是外部储存，什么是内部储存 ？
###### 1）、内部存储：
`/data `目录。一般我们使用 `getFilesDir()`  或`  getCacheDir() `方法获取本应用的内部储存路径，读写该路径下的文件不需要申请储存空间读写权限，且卸载应用时会自动删除。
###### 2）、外部存储：
`/storage ` 或 ` /mnt  `目录。一般我们使用 `getExternalStorageDirectory() ` 方法获取的路径来存取文件。

故在 Android 10 上即便拥有了储存空间的读写权限，也无法保证可以正常的进行文件的读写操作。
#### 二、规避 Android 10 适配
在 `AndroidManifest.xml` 中添加  `android:requestLegacyExternalStorage="true" `来请求使用旧的存储模式进行临时规避 Android 10 适配。但是存在缺陷，当 Android 11 正式上线之后，接踵而至的就是各种 bug 日志 反馈了，所以，还是踏踏实实做人，老老实实做事吧。
####  三、 适配
####（一）、系统相册回调 图片 地址适配
#### 1、适配 Android  系统版本号 >=4.4 &&<=10.0  版本
```
  /**
     * 适配Android 4.4及以上,根据uri获取图片的绝对路径
     *
     * @param context 上下文对象
     * @param uri     图片的Uri
     * @return 如果Uri对应的图片存在, 那么返回该图片的绝对路径, 否则返回null
     */
    @SuppressLint("NewApi")
    private static String getRealPathFromUriAboveApiAndroidK(Context context, Uri uri) {
        String filePath = null;
        if (DocumentsContract.isDocumentUri(context, uri)) {
            // 如果是document类型的 uri, 则通过document id来进行处理
            String documentId = DocumentsContract.getDocumentId(uri);
            if (isMediaDocument(uri)) {
                // 使用':'分割
                String id = documentId.split(":")[1];
                String selection = MediaStore.Images.Media._ID + "=?";
                String[] selectionArgs = {id};
                filePath = getDataColumn(context, MediaStore.Images.Media.EXTERNAL_CONTENT_URI, selection, selectionArgs);
            } else if (isDownloadsDocument(uri)) {
                Uri contentUri = ContentUris.withAppendedId(Uri.parse("content://downloads/public_downloads"), Long.valueOf(documentId));
                filePath = getDataColumn(context, contentUri, null, null);
            }
        } else if ("content".equalsIgnoreCase(uri.getScheme())) {
            // 如果是 content 类型的 Uri
            filePath = getDataColumn(context, uri, null, null);
        } else if ("file".equals(uri.getScheme())) {
            // 如果是 file 类型的 Uri,直接获取图片对应的路径
            filePath = uri.getPath();
        }
        return filePath;
    }

   /**
     * @param uri the Uri to check
     * @return Whether the Uri authority is MediaProvider
     */
    private static boolean isMediaDocument(Uri uri) {
        return "com.android.providers.media.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri the Uri to check
     * @return Whether the Uri authority is DownloadsProvider
     */
    private static boolean isDownloadsDocument(Uri uri) {
        return "com.android.providers.downloads.documents".equals(uri.getAuthority());
    }
```
这里相比于 4.4 系统以下设备的代码，已经明显的不一样了相对复杂了许多，需要自己主动去构建 路径， uri等，对用户友好了，但是对于开发者，也就那样吧。
#### 2、适配 Android  系统版本号 >= 10.0版本
使用系统相册进行图片选择时 。无法直接使用File，而应使用Uri。否则报错如下：
```
W/Glide: Load failed for /storage/emulated/0/Pictures/albumCameraImg/1591.jpg with size [549x549]

java.io.FileNotFoundException:  open failed: EACCES (Permission denied)
```
Uri 的获取方式使用到了`MediaStore`
获取图片代码为：
```
  /**
     * 适配 Android 10以上相册选取照片操作
     *
     * @param context 上下文
     * @param uri     图片uri
     * @return 图片地址
     */
    private static String getRealPathFromUriAboveApiAndroidQ(Context context, Uri uri) {
        Cursor cursor = null;
        String path = getRealPathFromUriAboveApiAndroidK(context, uri);
        try {
            cursor = context.getContentResolver().query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    new String[]{MediaStore.Images.Media._ID}, MediaStore.Images.Media.DATA + "=? ",
                    new String[]{path}, null);
            if (cursor != null && cursor.moveToFirst()) {
                int id = cursor.getInt(cursor.getColumnIndex(MediaStore.MediaColumns._ID));
                Uri baseUri = Uri.parse("content://media/external/images/media");
                return String.valueOf(Uri.withAppendedPath(baseUri, "" + id));
            } else {
                // 如果图片不在手机的共享图片数据库，就先把它插入。
                if (new File(path).exists()) {
                    ContentValues values = new ContentValues();
                    values.put(MediaStore.Images.Media.DATA, path);
                    return String.valueOf(context.getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values));
                } else {
                    return null;
                }
            }
        } catch (Exception e) {
            if (cursor != null)
                cursor.close();
        }
        return null;
    }

    /**
     * 适配Android 4.4以下(不包括api19),根据uri获取图片的绝对路径
     *
     * @param context 上下文对象
     * @param uri     图片的Uri
     * @return 如果Uri对应的图片存在, 那么返回该图片的绝对路径, 否则返回null
     */
    private static String getRealPathFromUriBelowApiAndroidK(Context context, Uri uri) {
        return getDataColumn(context, uri, null, null);
    }

    /**
     * 适配Android 4.4及以上,根据uri获取图片的绝对路径
     *
     * @param context 上下文对象
     * @param uri     图片的Uri
     * @return 如果Uri对应的图片存在, 那么返回该图片的绝对路径, 否则返回null
     */
    @SuppressLint("NewApi")
    private static String getRealPathFromUriAboveApiAndroidK(Context context, Uri uri) {
        String filePath = null;
        if (DocumentsContract.isDocumentUri(context, uri)) {
            // 如果是document类型的 uri, 则通过document id来进行处理
            String documentId = DocumentsContract.getDocumentId(uri);
            if (isMediaDocument(uri)) {
                // 使用':'分割
                String id = documentId.split(":")[1];
                String selection = MediaStore.Images.Media._ID + "=?";
                String[] selectionArgs = {id};
                filePath = getDataColumn(context, MediaStore.Images.Media.EXTERNAL_CONTENT_URI, selection, selectionArgs);
            } else if (isDownloadsDocument(uri)) {
                Uri contentUri = ContentUris.withAppendedId(Uri.parse("content://downloads/public_downloads"), Long.valueOf(documentId));
                filePath = getDataColumn(context, contentUri, null, null);
            }
        } else if ("content".equalsIgnoreCase(uri.getScheme())) {
            // 如果是 content 类型的 Uri
            filePath = getDataColumn(context, uri, null, null);
        } else if ("file".equals(uri.getScheme())) {
            // 如果是 file 类型的 Uri,直接获取图片对应的路径
            filePath = uri.getPath();
        }
        return filePath;
    }

/**
     * @param uri the Uri to check
     * @return Whether the Uri authority is MediaProvider
     */
    private static boolean isMediaDocument(Uri uri) {
        return "com.android.providers.media.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri the Uri to check
     * @return Whether the Uri authority is DownloadsProvider
     */
    private static boolean isDownloadsDocument(Uri uri) {
        return "com.android.providers.downloads.documents".equals(uri.getAuthority());
    }
```
大概也就是这样了，足可以让你玩转 Android 当前各个版本从系统相册内选择照片的需要。
#### （二）、视频系统相机
##### 1、Android 6.0  及以上设备 
6.0 及以上设备需要开发者 不仅仅在   ` AndroidManifest.xml `被注明必要权限，部分危险权限还需要开发者自己在代码逻辑中进行主动申请，这里很简单了，我只 推荐我用的一个框架，类似的权限申请框架还有很多，需要开发者自己主动发现了。[AndPermission](https://github.com/yanzhenjie/AndPermission)
AndPermission 链式调用，作者还进行了不少主动封装，也适配了 Androidx ，不过它也也存在不足，并且目前不知道作者是否还在自己进行维护，毕竟 issues 已经提了很多条了也没人维护。
##### 2、Android 8.0 及以上设备
###### (1)、8.0 及以上设备需要开发者在  ` AndroidManifest.xml `内注明  provider 了 如下
```
  <provider
            android:name="androidx.core.content.FileProvider"
            android:authorities="你的包名"
            android:exported="false"
            android:grantUriPermissions="true">
            <meta-data
                android:name="android.support.FILE_PROVIDER_PATHS"
                android:resource="@xml/file_paths" />
        </provider>
```
`authorities `：值一般是"项目的包名 + .provider"。当我们使用 `FileProvider `的 `getUriForFile `方法时参数需和 清单文件注册时的保持一致。
`exported `：是否对外开放，除非是对第三方提供服务，否则一般为false。
`grantUriPermissions `：是否授予临时权限，设置为true。
`resource:` 标签里面是用来指定共享的路径。就是我们的共享路径配置的 `xml `文件，可以自己命名。该文件放在 `res/xml `文件夹下，若没有 `xml `文件夹，创建一个。

######（2）、xml 文件内的内容：
```
<?xml version="1.0" encoding="utf-8"?>
<paths>
    <external-path
        name="external_files"
        path="Pictures/" />
</paths>
```
`<external-path> `可被替换成`<external-files-path>`、`<external-cache-path>`、`<file-path>`、`<cache-path>`等。下面给出五个的区别：
`<external-path>`：共享外部存储卡，对应/storage/emulated/0目录，即`Environment..getExternalStorageDirectory()`
`<external-files-path>`：共享外部存储的文件目录，对应/storage/emulated/0/Android/data/包名/files，即`Context.getExternalFilesDir()`
`<external-cache-path>`：共享外部存储的缓存目录，对应/storage/emulated/0/Android/data/包名/cache，即`Context.getExternalCacheDir()`
`<file-path>`：共享内部文件存储目录，对应 /data/data/包名/files目录，即`Context.getFilesDir()`
`<cache-path>`：共享内部缓存目录，对应 /data/data/包名/cache目录，即`Context.getCacheDir()`
`name`：随便定义
`path`：需要临时授权访问的路径。可以为空，表示指定目录下的所有文件、文件夹都可以被共享

举例：以上方代码为例，最后的物理路径为 /storage/emulated/0/Pictures。
如果将`<external-path>`换成`<file-path>`,则路径为： /data/data/包名/files/Pictures
如果将`<external-path>`换成`<cache-path>`,则路径为： /data/data/包名/cache/Pictures
######（3）、逻辑配置
```
//安全的获取临时 Uri
Uri imageUri = FileProvider.getUriForFile(context, "你的包名", fileName);
 //添加这一句表示对目标应用临时授权该Uri所代表的文件
intentCamera.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
```
##### 3、Android 10.0 及以上设备
```
Uri imageUri = createImageUri();
intentCamera.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

@RequiresApi(api = Build.VERSION_CODES.Q)
public Uri createImageUri() {
        ContentValues values = new ContentValues();
        // 需要指定文件信息时，非必须
        values.put(MediaStore.Images.Media.DESCRIPTION, "This is an image");
        values.put(MediaStore.Images.Media.DISPLAY_NAME, System.currentTimeMillis() + ".jpg");
        values.put(MediaStore.Images.Media.TITLE, System.currentTimeMillis() + ".jpg");
        values.put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/albumCameraImg");
        return getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
}
```
### 总结
Android 版本号每年都有改变，只需要不断适配 Google 对用户的各种考虑即可。奉上 demo 地址：[demo 地址](https://github.com/xiangshiweiyu/AlbumAndCameraSystem)
没有放上效果，但是绝对没问题，如果有问题请微信公众号联系我。

![欢迎关注](https://upload-images.jianshu.io/upload_images/6433394-36ed19e52086c960.jpg?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)