/**
 * 
 */
package lib.android.view.image;

import android.content.Context;
import android.content.pm.PackageManager;
import android.database.ContentObserver;
import android.database.Cursor;
import android.provider.MediaStore.Images.Media;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import lib.android.view.image.interfaces.ImageSelectHook;

/**
 * @author yanry
 *
 *         2016年1月15日
 */
public abstract class ImageSelectHandler {
	private ImageSelectHook hook;
	private List<File> selected;
	// album
	private List<File> allImages;
	private Map<File, List<File>> folderImages;

	public ImageSelectHandler() {
		selected = new LinkedList<File>();
	}

	public void setImageSelectHook(ImageSelectHook hook) {
		if (!hook.equals(this.hook)) {
			this.hook = hook;
			selected.clear();
		}
	}

	public void setSelectedImages(List<File> selected) {
		this.selected = selected;
	}

	public List<File> getSelectedImages() {
		return selected;
	}

	public ImageSelectHook getHook() {
		return hook;
	}

	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////// album

	public void initAlbum(final Context ctx, final FileFilter filter) {
		allImages = new LinkedList<File>();
		folderImages = new HashMap<File, List<File>>();
		ctx.getContentResolver().registerContentObserver(Media.EXTERNAL_CONTENT_URI, false, new ContentObserver(null) {
			@Override
			public void onChange(boolean selfChange) {
				loadData(ctx, filter);
			}
		});
		loadData(ctx, filter);
	}

	private synchronized void loadData(final Context ctx, final FileFilter filter) {
		folderImages.clear();
		if (ctx.checkCallingOrSelfPermission("android.permission.READ_EXTERNAL_STORAGE") == PackageManager.PERMISSION_GRANTED) {
			execute(new Runnable() {
				public void run() {
					// catch runtime java.lang.SecurityException.
					try {
						String[] projection = { Media.DATA };
						Cursor cs = null;
						cs = ctx.getContentResolver().query(Media.EXTERNAL_CONTENT_URI, projection, null, null,
								Media.DATE_MODIFIED + " desc");
						if (cs == null) {
							return;
						}
						List<File> all = new ArrayList<File>(cs.getCount());
						while (cs.moveToNext()) {
							File image = new File(cs.getString(0));
							if (filter != null && !filter.accept(image)) {
								continue;
							}
							all.add(image);
							File folder = image.getParentFile();
							List<File> images = folderImages.get(folder);
							if (images == null) {
								images = new LinkedList<File>();
								folderImages.put(folder, images);
							}
							images.add(image);
						}
						cs.close();
						allImages = all;
						onAlbumDataLoaded();
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			});
		}
	}

	public Set<File> getAlbumFolders() {
		if (folderImages != null) {
			return folderImages.keySet();
		}
		return null;
	}

	/**
	 * 
	 * @param folder
	 *            pass null to get all images.
	 * @return
	 */
	public List<File> getAlbumImages(File folder) {
		return folder == null ? allImages : folderImages.get(folder);
	}

	protected abstract void execute(Runnable r);

	/**
	 * This method is always invoked on worker thread.
	 */
	protected abstract void onAlbumDataLoaded();

}
