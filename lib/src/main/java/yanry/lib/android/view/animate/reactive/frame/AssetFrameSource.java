package yanry.lib.android.view.animate.reactive.frame;

import android.content.Context;
import android.content.res.AssetManager;
import android.text.TextUtils;

import androidx.annotation.NonNull;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Executor;

import yanry.lib.java.model.log.Logger;

/**
 * 存放在assets下的序列帧数据源。加载序列帧是按文件名自然排序，所以应该通过给文件名添加相同长度的序号（位数不足要在前面添加0）来控制播放顺序。
 */
public class AssetFrameSource implements AnimateFrameSource {
    private static AssetManager assetManager;
    private static HashMap<String, AssetFrameSource> cache = new HashMap<>();

    /**
     * 初始化，可以在程序启动时调用。
     *
     * @param context
     */
    public static void init(Context context) {
        assetManager = context.getAssets();
    }

    /**
     * 提前加载并缓存指定目录（递归）包含的序列帧，可避免在使用时才去加载导致动画响应不及时。
     *
     * @param assetsDir 以assets为基准的相对目录路径，该目录可包含多级子目录。
     * @param executor  线程池用于执行加载，若为null则在调用线程中执行。
     */
    public static void load(String assetsDir, Executor executor) {
        if (executor == null) {
            load(assetsDir);
        } else {
            executor.execute(() -> {
                load(assetsDir);
            });
        }
    }

    private static void load(String assetsDir) {
        try {
            long start = System.currentTimeMillis();
            File root = new File(assetsDir);
            loadDir(root, assetManager.list(root.getPath()));
            Logger.getDefault().dd("success load frames from assets dir: ", assetsDir, ", ", System.currentTimeMillis() - start, "ms");
        } catch (IOException e) {
            Logger.getDefault().catches(e);
        }
    }

    private static void loadDir(File folder, String[] fileList) throws IOException {
        if (folder == null || fileList == null) {
            return;
        }
        List<String> list = new ArrayList<>(fileList.length);
        for (String f : fileList) {
            File file = new File(folder, f);
            String[] subList = assetManager.list(file.getPath());
            if (subList != null) {
                if (subList.length == 0) {
                    // 文件
                    list.add(f);
                } else {
                    // 文件夹
                    loadDir(file, subList);
                }
            }
        }
        if (list.size() > 0) {
            cache.put(folder.getPath(), new AssetFrameSource(folder, list));
        }
    }

    /**
     * 获取指定assets下目录的序列帧数据源对象。
     *
     * @param assetsDirPath 以assets为基准的相对目录路径，该目录除了序列帧文件不应再包含子目录。
     * @return
     */
    public static AssetFrameSource get(@NonNull String assetsDirPath) {
        AssetFrameSource source = cache.get(assetsDirPath);
        if (source == null) {
            source = new AssetFrameSource(assetsDirPath);
            cache.put(assetsDirPath, source);
        }
        return source;
    }

    public static void clearCache(String assetsDir) {
        if (TextUtils.isEmpty(assetsDir)) {
            cache.clear();
        } else {
            if (!assetsDir.endsWith("/")) {
                assetsDir = assetsDir + "/";
            }
            Iterator<Map.Entry<String, AssetFrameSource>> iterator = cache.entrySet().iterator();
            while (iterator.hasNext()) {
                if (iterator.next().getKey().startsWith(assetsDir)) {
                    iterator.remove();
                }
            }
        }
    }

    private File dir;
    private List<String> fileList;

    private AssetFrameSource(File dir, List<String> fileList) {
        this.dir = dir;
        this.fileList = fileList;
    }

    private AssetFrameSource(String dirPath) {
        this.dir = new File(dirPath);
        try {
            fileList = Arrays.asList(assetManager.list(dirPath));
        } catch (IOException e) {
            Logger.getDefault().catches(e);
        }
    }

    @Override
    public int getFrameCount() {
        if (fileList != null) {
            return fileList.size();
        }
        return 0;
    }

    @Override
    public InputStream getFrameInputStream(int index) {
        if (fileList != null && index < fileList.size()) {
            try {
                return assetManager.open(new File(dir, fileList.get(index)).getPath());
            } catch (IOException e) {
                Logger.getDefault().catches(e);
            }
        }
        return null;
    }

    @Override
    public String toString() {
        return dir.getPath();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AssetFrameSource that = (AssetFrameSource) o;
        return dir.equals(that.dir);
    }

    @Override
    public int hashCode() {
        return Objects.hash(dir);
    }
}
