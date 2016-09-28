/**
 *
 */
package lib.android.view.image.preview;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;

import com.lib.android.R;

import lib.android.model.bitmap.BitmapLoader;
import lib.android.model.bitmap.LoadHook;
import uk.co.senab.photoview.PhotoView;
import uk.co.senab.photoview.PhotoViewAttacher.OnPhotoTapListener;

/**
 * @author yanry
 *         <p/>
 *         2016年1月24日
 */
public abstract class ImagePreviewHandler implements OnPageChangeListener {

	private PreviewImageData data;
	private int currentPos;
	private HackyViewPager vp;
	private FragmentManager fm;

	public PreviewImageData getData() {
		return data;
	}

	public void setData(PreviewImageData data) {
		this.data = data;
	}

	public int getCurrentPosition() {
		return currentPos;
	}

	public void setView(HackyViewPager vp, FragmentManager fm) {
		this.vp = vp;
		this.fm = fm;
		currentPos = data.getInitialPosition();
		setAdapter();
		vp.addOnPageChangeListener(this);
	}

	private void setAdapter() {
		FragmentStatePagerAdapter adapter = new FragmentStatePagerAdapter(fm) {

			@Override
			public int getCount() {
				return data.getCount();
			}

			@Override
			public Fragment getItem(int position) {
				return new ImagePreviewFragment(position);
			}

			@Override
			public void notifyDataSetChanged() {
				super.notifyDataSetChanged();
			}

		};
		vp.setAdapter(adapter);
		vp.setCurrentItem(currentPos);
	}

	public void deleteCurrentImage() {
		data.delete(currentPos);
		if (currentPos == data.getCount()) {
			currentPos--;
		}
		setAdapter();
	}

	@Override
	public void onPageScrollStateChanged(int arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onPageScrolled(int arg0, float arg1, int arg2) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onPageSelected(int arg0) {
		currentPos = arg0;
	}

	protected abstract BitmapLoader getBitmapLoader();

	protected abstract int getDefaultImageResId();

	/**
	 * Usually show error message to user.
	 */
	protected abstract void onLoadImageError();

	private class ImagePreviewFragment extends Fragment implements OnPhotoTapListener {
		private int position;
		private boolean hasThumb;

		private ImagePreviewFragment(int position) {
			this.position = position;
		}

		@Override
		public void onDestroyView() {
			hasThumb = false;
			super.onDestroyView();
		}

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
			View itemView = LayoutInflater.from(getActivity()).inflate(R.layout.fragment_image_preview, container,
					false);
			final PhotoView pv = (PhotoView) itemView.findViewById(R.id.image_preview_pv);
			data.initPhotoView(pv, position, getActivity());
			pv.setOnPhotoTapListener(this);
			final ProgressBar pb = (ProgressBar) itemView.findViewById(R.id.image_preview_pb);
			String thumb = data.getThumb(position);
			if (thumb != null) {
				getBitmapLoader().getRequest(thumb).bind(getActivity()).load(new LoadHook() {
					
					@Override
					public boolean onStartLoading() {
						pv.setImageResource(getDefaultImageResId());
						// skip loading thumb if cache does not exist
						return false;
					}
					
					@Override
					public void onShow(Bitmap bmp) {
						pv.setImageBitmap(bmp);
						hasThumb = true;
					}
					
					@Override
					public void onError() {
					}
					
					@Override
					public boolean isAbort() {
						return false;
					}
				}).commit();
			}
			getBitmapLoader().getRequest(data.getImage(position)).load(new LoadHook() {
				
				@Override
				public boolean onStartLoading() {
					pb.setVisibility(View.VISIBLE);
					return true;
				}
				
				@Override
				public void onShow(Bitmap bmp) {
					pb.setVisibility(View.GONE);
					pv.setImageBitmap(bmp);
				}
				
				@Override
				public void onError() {
					pb.setVisibility(View.GONE);
					if (!hasThumb) {
						pv.setImageResource(getDefaultImageResId());
					}
					onLoadImageError();
				}
				
				@Override
				public boolean isAbort() {
					return false;
				}
			}).fitSize(1, 1).bind(getActivity()).commit();
			return itemView;
		}

		@Override
		public void onPhotoTap(View view, float x, float y) {
			getActivity().finish();
		}
	}
}
