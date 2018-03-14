# DSQLiteCache
ASimpleCache是一个优秀的缓存框架。
1、可以缓存各类数据，比喻：字符串、JsonObject、JsonArray、Bitmap、Drawable、序列化的java对象，和 byte数据。
2、轻，轻到只有一个JAVA文件。
3、可配置，可以配置缓存路径，缓存大小，缓存数量等。
4、可以设置缓存超时时间，缓存超时自动失效，并被删除。
5、支持多进程。
等等

怎么使用呢？
ACache.get(MainActivity.this).put("username_key", "admin");
//保存10秒，如果超过10秒去获取这个key，将为null
ACache.get(MainActivity.this).put("password_key", "123456789", 10);
//保存两天，如果超过两天去获取这个key，将为null
ACache.get(MainActivity.this).put("password_key", "123456789", 2 * ACache.TIME_DAY);

简单解析此原理：
项目源码一个类ACache，其实是多个类压缩而来，其内部有多个内部类。因不需要对外公开。

初始化：
当初次调用ACache.get(MainActivity.this)方法时，
其内部会调用File f = new File(ctx.getCacheDir(), "ACache")，代码，获取缓存目录，当然你也可以指定缓存目录位置。
然后创建一个此目录的管理工具类，并启动线程扫描此目录下的所有文件，并把文件对象与最后修改时间作为键值对保存在内存的HashMap中。

put：
当调用ACache对象的put方法时，以key字符串的hashCode值作为文件名，以value参数的字节数组值作为内容写入文件中。
如果设置了保存时间，则把当前毫秒数+保存时间的毫秒数+"- "保存到文件内容的开头处。

get：
当需要获取值时，首先获取key的hashCode值作为文件名所对应的File对象，然后读取内容。
先获取前面的时间数据，看是否有或者是否过期，以决定是否需要返回数据到上层。
在获取文件的正式内容，再转换成字符串或者对象等类型，返回到上层。


ASimpleCache框架的以上分析，大概是这样。
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

文本重点是介绍我的DSQLiteCache框架。
其实呢，就是在上述巨作的基础上修改而来的，项目名称为DSQLiteCache，原谅我用了字母D。

DSQLiteCache项目技术原理：
上述大神开发的ACache项目中，是把缓存文件全部获取到，然后保存在内存的HashMap中。个人感觉会占用大量app内存，
上述大神开发的ACache项目中，使用了大量的同步代码，以及新开线程扫描数据。个人感觉在多线程情况下可能会有问题。

故，我的缓存数据都是以二进制的形式保存到SQLite数据库中了，避免占用大量内存。
我使用了ContentProvider作为数据访问媒介，以避免多线程并发问题。
我没有把所有的代码压到一个类中。对外暴露的用户接口类为DCache.java，以区分ACache.java。
因为使用了ContentProvider，所以在获取接口实例时不需要传入上下文了。

继承时注意：
1、使用aar包或者引入源码，都可以。注意一点，如果引入源码的话，注意同时引入AndroidManifest.xml中provider注册代码。
如果android:authorities=""值变化了，需同时修改DCache.java中的authorities常量值。

使用方法：
DCache.get().put(String key, JSONObject o);
DCache.get().put(String key, JSONObject o, long saveTime);

DCache.get().put(String key, byte[] data);
DCache.get().put(String key, byte[] data, long saveTime);

DCache.get().put(String key, String data);
DCache.get().put(String key, String data, long saveTime);

简单吧，不多说。

保存的数据 支持的类型跟ACache一样，字符串、JsonObject、JsonArray、Bitmap、Drawable、序列化的java对象，和 byte数据。
DCache对外暴露的用户接口直接借鉴ACache的了。
