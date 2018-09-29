/**
 *
 */
package lib.android.model.bitmap;

import android.content.ContentResolver;
import android.content.res.Resources;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.text.Html;
import android.util.LruCache;
import android.widget.TextView;

import java.io.File;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

import lib.android.model.NetworkConnMngr;
import lib.android.model.NetworkConnMngr.ConnectivityListener;
import lib.android.model.bitmap.access.FileBitmapAccess;
import lib.android.model.bitmap.access.Level2BlobAccess;
import lib.android.model.bitmap.access.Level2FileAccess;
import lib.android.model.bitmap.access.ResBitmapAccess;
import lib.android.model.bitmap.access.UriBitmapAccess;
import lib.android.model.bitmap.access.UrlBlobBitmapAccess;
import lib.android.model.bitmap.access.UrlFileBitmapAccess;
import lib.android.model.dao.AndroidBaseDao;
import lib.common.model.cache.TimerObjectPool;
import lib.common.model.resourceaccess.AccessHook;
import lib.common.model.resourceaccess.CacheResourceAccess;
import lib.common.model.resourceaccess.FileHashMapper;
import lib.common.model.resourceaccess.UrlFileAccess;
import lib.common.model.resourceaccess.UrlOption;
import lib.common.model.task.AdvancedExecutor;
import lib.common.util.FileUtil;

/**
 * @author yanry
 * <p>
 * 2016年5月7日
 */
public class BitmapLoader implements ConnectivityListener {
    FileBitmapAccess fileAccess;
    UriBitmapAccess uriAccess;
    UrlFileBitmapAccess urlFileAccess;
    UrlBlobBitmapAccess urlBlobAccess;
    ResBitmapAccess resAccess;
    TimerObjectPool<BitmapRequest> requestPool;
    private LruCache<Object, Bitmap> cache;
    private ThreadPoolExecutor executor;
    private UrlFileAccess htmlImageAccess;
    private Level2BlobAccess lv2BlobAccess;
    private Level2FileAccess lv2FileAccess;

    /**
     * @param maxMemorySize  max memory cache size in byte, pass 0 to use default size.
     * @param threadNumber
     * @param maxConcurrency max number of pending {@link BitmapRequest}, excessive
     *                       requests will cause oldest requests to be discarded.
     */
    public BitmapLoader(int maxMemorySize, int threadNumber, int maxConcurrency) {
        this(new LruCache<Object, Bitmap>(
                maxMemorySize > 0 ? maxMemorySize : (int) (Runtime.getRuntime().maxMemory() / 8)) {
            @Override
            protected int sizeOf(Object key, Bitmap value) {
                return value.getByteCount();
            }
        }, threadNumber, maxConcurrency);
    }

    public BitmapLoader(LruCache<Object, Bitmap> cache, int threadNumber, int maxConcurrency) {
        this.cache = cache;
        executor = new AdvancedExecutor(threadNumber, 120, true, null, maxConcurrency);
        fileAccess = new FileBitmapAccess() {

            @Override
            protected Executor getGenerationExecutor() {
                return executor;
            }

            @Override
            protected LruCache<Object, Bitmap> getCache() {
                return BitmapLoader.this.cache;
            }
        };
        requestPool = new TimerObjectPool<BitmapRequest>(120) {

            @Override
            protected void release(BitmapRequest obj) {
            }

            @Override
            protected BitmapRequest generate() {
                return new BitmapRequest(BitmapLoader.this);
            }
        };
    }

    public ResBitmapAccess supportRes(final Resources res) {
        resAccess = new ResBitmapAccess() {

            @Override
            protected Executor getGenerationExecutor() {
                return executor;
            }

            @Override
            protected LruCache<Object, Bitmap> getCache() {
                return cache;
            }

            @Override
            protected Resources getResources() {
                return res;
            }
        };
        return resAccess;
    }

    public UriBitmapAccess supportUri(final ContentResolver cr) {
        uriAccess = new UriBitmapAccess() {

            @Override
            protected Executor getGenerationExecutor() {
                return executor;
            }

            @Override
            protected LruCache<Object, Bitmap> getCache() {
                return cache;
            }

            @Override
            protected ContentResolver getContentResolver() {
                return cr;
            }
        };
        return uriAccess;
    }

    public UrlFileBitmapAccess supportUrlInFile(FileHashMapper fileMapper,
                                                final NetworkConnMngr conn, final boolean supportResume) {
        lv2FileAccess = new Level2FileAccess(fileMapper) {

            @Override
            protected boolean supportResume() {
                return supportResume;
            }
        };
        urlFileAccess = new UrlFileBitmapAccess() {

            @Override
            protected boolean isConnected() {
                return conn == null ? true : conn.isConnected();
            }

            @Override
            protected Executor getGenerationExecutor() {
                return executor;
            }

            @Override
            protected String getPath(CacheKey<String> key) {
                // need not to rotate bitmap from URL.
                return null;
            }

            @Override
            protected LruCache<Object, Bitmap> getCache() {
                return cache;
            }

            @Override
            protected CacheResourceAccess<String, File, UrlOption, AccessHook<File>> getLevel2Access() {
                return lv2FileAccess;
            }
        };
        urlBlobAccess = null;
        lv2BlobAccess = null;
        return urlFileAccess;
    }

    public UrlBlobBitmapAccess supportUrlInBlob(final NetworkConnMngr conn, AndroidBaseDao dao,
                                                String table, String keyField, String blobField) {
        lv2BlobAccess = new Level2BlobAccess(dao, table, keyField, blobField);
        urlBlobAccess = new UrlBlobBitmapAccess() {

            @Override
            protected Executor getGenerationExecutor() {
                return executor;
            }

            @Override
            protected boolean isConnected() {
                return conn == null ? true : conn.isConnected();
            }

            @Override
            protected LruCache<Object, Bitmap> getCache() {
                return cache;
            }

            @Override
            protected CacheResourceAccess<String, byte[], UrlOption, AccessHook<byte[]>> getLevel2Access() {
                return lv2BlobAccess;
            }
        };
        urlFileAccess = null;
        lv2FileAccess = null;
        return urlBlobAccess;
    }

    public UrlFileAccess supportHtmlImage(FileHashMapper mapper) {
        htmlImageAccess = new UrlFileAccess(mapper) {

            @Override
            protected Executor getGenerationExecutor() {
                return executor;
            }

            @Override
            protected boolean supportResume() {
                // resume is usually not supported in HTML.
                return false;
            }
        };
        return htmlImageAccess;
    }

    public Level2FileAccess getLevel2FileAccess() {
        return lv2FileAccess;
    }

    public BitmapRequest getRequest(Object src) {
        return requestPool.obtain().from(src);
    }

    public void loadHtmlImage(String source, TextView tv, Drawable defaultDrawable) {
        tv.setText(Html.fromHtml(source, new HtmlImageGetter(tv, defaultDrawable, htmlImageAccess), null));
    }

    public long getLocalCacheSize(boolean clear, Set<File> exclusive) {
        long size = 0;
        if (urlFileAccess != null) {
            size += FileUtil.getDirSize(lv2FileAccess.getMapper().getRootDir(), clear, exclusive);
        }
        if (urlBlobAccess != null) {
            size += lv2BlobAccess.getDao().getDbFileLength();
            if (clear) {
                SQLiteDatabase db = lv2BlobAccess.getDao().getDatabase(true);
                db.beginTransactionNonExclusive();
                db.delete(lv2BlobAccess.getTable(), null, null);
                db.setTransactionSuccessful();
                db.endTransaction();
            }
        }
        if (htmlImageAccess != null) {
            size += FileUtil.getDirSize(htmlImageAccess.getMapper().getRootDir(), clear, exclusive);
        }
        return size;
    }

    public ThreadPoolExecutor getExecutor() {
        return executor;
    }

    public LruCache<Object, Bitmap> getCache() {
        return cache;
    }

    @Override
    public void onConnected(String typeName) {
        if (urlFileAccess != null) {
            urlFileAccess.flushTaskCache();
        } else if (urlBlobAccess != null) {
            urlBlobAccess.flushTaskCache();
        }
    }

    @Override
    public void onDisconnect() {
        // TODO Auto-generated method stub

    }
}
