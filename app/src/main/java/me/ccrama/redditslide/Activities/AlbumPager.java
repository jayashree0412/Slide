package me.ccrama.redditslide.Activities;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.cocosw.bottomsheet.BottomSheet;
import com.devspark.robototextview.RobotoTypefaces;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.assist.FailReason;
import com.nostra13.universalimageloader.core.assist.ImageScaleType;
import com.nostra13.universalimageloader.core.imageaware.ImageViewAware;
import com.nostra13.universalimageloader.core.listener.ImageLoadingListener;
import com.nostra13.universalimageloader.core.listener.ImageLoadingProgressListener;
import com.sothree.slidinguppanel.SlidingUpPanelLayout;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import me.ccrama.redditslide.Adapters.ImageGridAdapter;
import me.ccrama.redditslide.Fragments.BlankFragment;
import me.ccrama.redditslide.Fragments.FolderChooserDialogCreate;
import me.ccrama.redditslide.Fragments.SubmissionsView;
import me.ccrama.redditslide.ImgurAlbum.AlbumUtils;
import me.ccrama.redditslide.ImgurAlbum.Image;
import me.ccrama.redditslide.Notifications.ImageDownloadNotificationService;
import me.ccrama.redditslide.R;
import me.ccrama.redditslide.Reddit;
import me.ccrama.redditslide.SettingValues;
import me.ccrama.redditslide.SpoilerRobotoTextView;
import me.ccrama.redditslide.Views.ExoVideoView;
import me.ccrama.redditslide.Views.ImageSource;
import me.ccrama.redditslide.Views.SubsamplingScaleImageView;
import me.ccrama.redditslide.Views.ToolbarColorizeHelper;
import me.ccrama.redditslide.Visuals.ColorPreferences;
import me.ccrama.redditslide.Visuals.FontPreferences;
import me.ccrama.redditslide.util.BlendModeUtil;
import me.ccrama.redditslide.util.DialogUtil;
import me.ccrama.redditslide.util.FileUtil;
import me.ccrama.redditslide.util.GifUtils;
import me.ccrama.redditslide.util.LinkUtil;
import me.ccrama.redditslide.util.NetworkUtil;
import me.ccrama.redditslide.util.ShareUtil;
import me.ccrama.redditslide.util.SubmissionParser;

import static me.ccrama.redditslide.Notifications.ImageDownloadNotificationService.EXTRA_SUBMISSION_TITLE;


/**
 * Created by ccrama on 1/25/2016. <p/> This is an extension of Album.java which utilizes a
 * ViewPager for Imgur content instead of a RecyclerView (horizontal vs vertical). It also supports
 * gifs and progress bars which Album.java doesn't.
 */
public class AlbumPager extends FullScreenActivity
        implements FolderChooserDialogCreate.FolderCallback {

    private static int adapterPosition;
    public static final String SUBREDDIT = "subreddit";

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == android.R.id.home) {
            onBackPressed();
        }
        if (id == R.id.vertical) {
            SettingValues.albumSwipe = false;
            SettingValues.prefs.edit().putBoolean(SettingValues.PREF_ALBUM_SWIPE, false).apply();
            Intent i = new Intent(AlbumPager.this, Album.class);
            if (getIntent().hasExtra(MediaView.SUBMISSION_URL)) {
                i.putExtra(MediaView.SUBMISSION_URL,
                        getIntent().getStringExtra(MediaView.SUBMISSION_URL));
            }
            if(getIntent().hasExtra(SUBREDDIT)){
                i.putExtra(SUBREDDIT, getIntent().getStringExtra(SUBREDDIT));
            }
            if (getIntent().hasExtra(EXTRA_SUBMISSION_TITLE)) {
                i.putExtra(EXTRA_SUBMISSION_TITLE, getIntent().getStringExtra(EXTRA_SUBMISSION_TITLE));
            }
            i.putExtras(getIntent());
            startActivity(i);
            finish();
        }
        if (id == R.id.grid) {
            mToolbar.findViewById(R.id.grid).callOnClick();
        }
        if (id == R.id.external) {
            LinkUtil.openExternally(getIntent().getExtras().getString("url", ""));
        }

        if (id == R.id.comments) {
            int adapterPosition = getIntent().getIntExtra(MediaView.ADAPTER_POSITION, -1);
            finish();
            SubmissionsView.dataChanged(adapterPosition);
            //getIntent().getStringExtra(MediaView.SUBMISSION_SUBREDDIT));
            //SubmissionAdapter.setOpen(this, getIntent().getStringExtra(MediaView.SUBMISSION_URL));
        }

        if (id == R.id.download && images != null) {
            int index = 0;
            for (final Image elem : images) {
                doImageSave(false, elem.getImageUrl(), index);
                index++;
            }
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 3) {
            Reddit.appRestart.edit().putBoolean("tutorialSwipe", true).apply();
        }
    }

    public String subreddit;

    public void onCreate(Bundle savedInstanceState) {
        overrideSwipeFromAnywhere();
        super.onCreate(savedInstanceState);
        getTheme().applyStyle(
                new ColorPreferences(this).getDarkThemeSubreddit(ColorPreferences.FONT_STYLE),
                true);
        setContentView(R.layout.album_pager);

        //Keep the screen on
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        if(getIntent().hasExtra(SUBREDDIT)){
            this.subreddit = getIntent().getStringExtra(SUBREDDIT);
        }

        mToolbar = (Toolbar) findViewById(R.id.toolbar);
        mToolbar.setTitle(R.string.type_album);
        ToolbarColorizeHelper.colorizeToolbar(mToolbar, Color.WHITE, this);
        setSupportActionBar(mToolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mToolbar.setPopupTheme(
                new ColorPreferences(this).getDarkThemeSubreddit(ColorPreferences.FONT_STYLE));

        adapterPosition = getIntent().getIntExtra(MediaView.ADAPTER_POSITION, -1);

        String url = getIntent().getExtras().getString("url", "");
        setShareUrl(url);
        pagerLoad = new LoadIntoPager(url, this);
        pagerLoad.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

        if (!Reddit.appRestart.contains("tutorialSwipe")) {
            startActivityForResult(new Intent(this, SwipeTutorial.class), 3);
        }

    }

    LoadIntoPager pagerLoad;

    public class LoadIntoPager extends AlbumUtils.GetAlbumWithCallback {

        String url;

        public LoadIntoPager(@NonNull String url, @NonNull Activity baseActivity) {
            super(url, baseActivity);
            this.url = url;
        }

        @Override
        public void onError() {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    try {
                        new AlertDialog.Builder(AlbumPager.this)
                                .setTitle(R.string.error_album_not_found)
                                .setMessage(R.string.error_album_not_found_text)
                                .setNegativeButton(R.string.btn_no, (dialog, which) ->
                                        finish())
                                .setCancelable(false)
                                .setPositiveButton(R.string.btn_yes, (dialog, which) -> {
                                    Intent i = new Intent(AlbumPager.this, Website.class);
                                    i.putExtra(LinkUtil.EXTRA_URL, url);
                                    startActivity(i);
                                    finish();
                                })
                                .show();
                    } catch (Exception e) {

                    }
                }
            });

        }

        @Override
        public void doWithData(final List<Image> jsonElements) {
            super.doWithData(jsonElements);
            findViewById(R.id.progress).setVisibility(View.GONE);
            images = new ArrayList<>(jsonElements);

            p = (ViewPager) findViewById(R.id.images_horizontal);

            if (getSupportActionBar() != null) {
                getSupportActionBar().setSubtitle(1 + "/" + images.size());
            }

            AlbumViewPagerAdapter adapter = new AlbumViewPagerAdapter(getSupportFragmentManager());
            p.setAdapter(adapter);
            p.setCurrentItem(1);
            findViewById(R.id.grid).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    LayoutInflater l = getLayoutInflater();
                    View body = l.inflate(R.layout.album_grid_dialog, null, false);
                    GridView gridview = body.findViewById(R.id.images);
                    gridview.setAdapter(new ImageGridAdapter(AlbumPager.this, images));

                    final AlertDialog.Builder b = new AlertDialog.Builder(AlbumPager.this)
                            .setView(body);
                    final Dialog d = b.create();
                    gridview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                        public void onItemClick(AdapterView<?> parent, View v, int position,
                                long id) {
                            p.setCurrentItem(position + 1);
                            d.dismiss();
                        }
                    });
                    d.show();
                }
            });
            p.addOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
                @Override
                public void onPageScrolled(int position, float positionOffset,
                        int positionOffsetPixels) {
                    if (position != 0) {
                        if (getSupportActionBar() != null) {
                            getSupportActionBar().setSubtitle((position) + "/" + images.size());
                        }
                    }
                    if (position == 0 && positionOffset < 0.2) {
                        finish();
                    }
                }
            });
            adapter.notifyDataSetChanged();

        }
    }

    ViewPager p;

    public List<Image> images;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.album_pager, menu);
        adapterPosition = getIntent().getIntExtra(MediaView.ADAPTER_POSITION, -1);
        if (adapterPosition < 0) {
            menu.findItem(R.id.comments).setVisible(false);
        }
        return true;
    }

    private class AlbumViewPagerAdapter extends FragmentStatePagerAdapter {

        AlbumViewPagerAdapter(FragmentManager m) {
            super(m, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);
        }

        @NonNull
        @Override
        public Fragment getItem(int i) {

            if (i == 0) {
                return new BlankFragment();
            }

            i--;
            Image current = images.get(i);

            if (current.isAnimated()) {
                //do gif stuff
                Fragment f = new Gif();
                Bundle args = new Bundle();
                args.putInt("page", i);
                f.setArguments(args);

                return f;
            } else {
                Fragment f = new ImageFullNoSubmission();
                Bundle args = new Bundle();
                args.putInt("page", i);
                f.setArguments(args);

                return f;
            }
        }

        @Override
        public int getCount() {
            if (images == null) {
                return 0;
            }
            return images.size() + 1;
        }
    }

    public static class Gif extends Fragment {

        private int i = 0;
        private View gif;
        ViewGroup   rootView;
        ProgressBar loader;

        @Override
        public void setUserVisibleHint(boolean isVisibleToUser) {
            super.setUserVisibleHint(isVisibleToUser);
            if (this.isVisible()) {
                if (!isVisibleToUser)   // If we are becoming invisible, then...
                {
                    ((ExoVideoView) gif).pause();
                    gif.setVisibility(View.GONE);
                }

                if (isVisibleToUser) // If we are becoming visible, then...
                {
                    ((ExoVideoView) gif).play();
                    gif.setVisibility(View.VISIBLE);

                }
            }
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
            rootView = (ViewGroup) inflater.inflate(R.layout.submission_gifcard_album, container,
                    false);
            loader = rootView.findViewById(R.id.gifprogress);


            gif = rootView.findViewById(R.id.gif);

            gif.setVisibility(View.VISIBLE);
            final ExoVideoView v = (ExoVideoView) gif;
            v.clearFocus();

            final String url = ((AlbumPager) getActivity()).images.get(i).getImageUrl();

            new GifUtils.AsyncLoadGif(getActivity(), rootView.findViewById(R.id.gif), loader, null, null,
                    false, true, rootView.findViewById(R.id.size),
                    ((AlbumPager)getActivity()).subreddit, getActivity().getIntent().getStringExtra(EXTRA_SUBMISSION_TITLE)).execute(url);
            rootView.findViewById(R.id.more).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ((AlbumPager) getActivity()).showBottomSheetImage(url, true, i);
                }
            });
            rootView.findViewById(R.id.save).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    MediaView.doOnClick.run();
                }
            });
            if (!SettingValues.imageDownloadButton) {
                rootView.findViewById(R.id.save).setVisibility(View.INVISIBLE);
            }
            return rootView;
        }


        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            Bundle bundle = this.getArguments();
            i = bundle.getInt("page", 0);

        }

    }

    public void showBottomSheetImage(final String contentUrl, final boolean isGif,
            final int index) {

        int[] attrs = new int[]{R.attr.tintColor};
        TypedArray ta = obtainStyledAttributes(attrs);

        int color = ta.getColor(0, Color.WHITE);
        Drawable external = getResources().getDrawable(R.drawable.ic_open_in_browser);
        Drawable share = getResources().getDrawable(R.drawable.ic_share);
        Drawable image = getResources().getDrawable(R.drawable.ic_image);
        Drawable save = getResources().getDrawable(R.drawable.ic_download);

        final List<Drawable> drawableSet = Arrays.asList(external, share, image, save);
        BlendModeUtil.tintDrawablesAsSrcAtop(drawableSet, color);

        ta.recycle();
        BottomSheet.Builder b = new BottomSheet.Builder(this).title(contentUrl);

        b.sheet(2, external, getString(R.string.open_externally));
        b.sheet(5, share, getString(R.string.submission_link_share));
        if (!isGif) b.sheet(3, image, getString(R.string.share_image));
        b.sheet(4, save, getString(R.string.submission_save_image));
        b.listener(new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case (2): {
                        LinkUtil.openExternally(contentUrl);
                    }
                    break;
                    case (3): {
                        ShareUtil.shareImage(contentUrl, AlbumPager.this);
                    }
                    break;
                    case (5): {
                        Reddit.defaultShareText("", contentUrl, AlbumPager.this);
                    }
                    break;
                    case (4): {
                        doImageSave(isGif, contentUrl, index);
                    }
                    break;
                }
            }
        });

        b.show();

    }

    public void doImageSave(boolean isGif, String contentUrl, int index) {
        if (!isGif) {
            if (Reddit.appRestart.getString("imagelocation", "").isEmpty()) {
                showFirstDialog();
            } else if (!new File(Reddit.appRestart.getString("imagelocation", "")).exists()) {
                showErrorDialog();
            } else {
                Intent i = new Intent(this, ImageDownloadNotificationService.class);
                i.putExtra("actuallyLoaded", contentUrl);
                if (subreddit != null && !subreddit.isEmpty()) i.putExtra("subreddit", subreddit);
                if (getIntent().hasExtra(EXTRA_SUBMISSION_TITLE)) {
                    i.putExtra(EXTRA_SUBMISSION_TITLE, getIntent().getStringExtra(EXTRA_SUBMISSION_TITLE));
                }
                i.putExtra("index", index);
                startService(i);
            }
        } else {
            MediaView.doOnClick.run();
        }
    }

    public static class ImageFullNoSubmission extends Fragment {

        private int i = 0;

        public ImageFullNoSubmission() {

        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
            final ViewGroup rootView =
                    (ViewGroup) inflater.inflate(R.layout.album_image_pager, container, false);

            if(((AlbumPager) getActivity()).images == null){
                ((AlbumPager) getActivity()).pagerLoad.onError();
            } else {
                final Image current = ((AlbumPager) getActivity()).images.get(i);
                final String url = current.getImageUrl();
                boolean lq = false;
                if (SettingValues.loadImageLq && (SettingValues.lowResAlways || (!NetworkUtil.isConnectedWifi(getActivity())
                        && SettingValues.lowResMobile))) {
                    String lqurl = url.substring(0, url.lastIndexOf("."))
                            + (SettingValues.lqLow ? "m" : (SettingValues.lqMid ? "l" : "h"))
                            + url.substring(url.lastIndexOf("."));
                    loadImage(rootView, this, lqurl, ((AlbumPager) getActivity()).images.size() == 1);
                    lq = true;
                } else {
                    loadImage(rootView, this, url, ((AlbumPager) getActivity()).images.size() == 1);
                }

                {
                    rootView.findViewById(R.id.more).setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            ((AlbumPager) getActivity()).showBottomSheetImage(url, false, i);
                        }
                    });
                    {
                        rootView.findViewById(R.id.save).setOnClickListener(new View.OnClickListener() {

                            @Override
                            public void onClick(View v2) {
                                ((AlbumPager) getActivity()).doImageSave(false, url, i);
                            }

                        });
                        if (!SettingValues.imageDownloadButton) {
                            rootView.findViewById(R.id.save).setVisibility(View.INVISIBLE);
                        }
                    }


                }
                {
                    String title = "";
                    String description = "";
                    if (current.getTitle() != null) {
                        List<String> text = SubmissionParser.getBlocks(current.getTitle());
                        title = text.get(0).trim();
                    }

                    if (current.getDescription() != null) {
                        List<String> text = SubmissionParser.getBlocks(current.getDescription());
                        description = text.get(0).trim();
                    }
                    if (title.isEmpty() && description.isEmpty()) {
                        rootView.findViewById(R.id.panel).setVisibility(View.GONE);
                        (rootView.findViewById(R.id.margin)).setPadding(0, 0, 0, 0);
                    } else if (title.isEmpty()) {
                        LinkUtil.setTextWithLinks(description, rootView.findViewById(R.id.title));
                    } else {
                        LinkUtil.setTextWithLinks(title, rootView.findViewById(R.id.title));
                        LinkUtil.setTextWithLinks(description, rootView.findViewById(R.id.body));
                    }
                    {
                        int type = new FontPreferences(getContext()).getFontTypeComment().getTypeface();
                        Typeface typeface;
                        if (type >= 0) {
                            typeface = RobotoTypefaces.obtainTypeface(getContext(), type);
                        } else {
                            typeface = Typeface.DEFAULT;
                        }
                        ((SpoilerRobotoTextView) rootView.findViewById(R.id.body)).setTypeface(
                                typeface);
                    }
                    {
                        int type = new FontPreferences(getContext()).getFontTypeTitle().getTypeface();
                        Typeface typeface;
                        if (type >= 0) {
                            typeface = RobotoTypefaces.obtainTypeface(getContext(), type);
                        } else {
                            typeface = Typeface.DEFAULT;
                        }
                        ((SpoilerRobotoTextView) rootView.findViewById(R.id.title)).setTypeface(
                                typeface);
                    }
                    final SlidingUpPanelLayout l = rootView.findViewById(R.id.sliding_layout);
                    rootView.findViewById(R.id.title).setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            l.setPanelState(SlidingUpPanelLayout.PanelState.EXPANDED);
                        }
                    });
                    rootView.findViewById(R.id.body).setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            l.setPanelState(SlidingUpPanelLayout.PanelState.EXPANDED);
                        }
                    });
                }
                if (lq) {
                    rootView.findViewById(R.id.hq).setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            loadImage(rootView, ImageFullNoSubmission.this, url,
                                    ((AlbumPager) getActivity()).images.size() == 1);
                            rootView.findViewById(R.id.hq).setVisibility(View.GONE);
                        }
                    });
                } else {
                    rootView.findViewById(R.id.hq).setVisibility(View.GONE);
                }

                if (getActivity().getIntent().hasExtra(MediaView.SUBMISSION_URL)) {
                    rootView.findViewById(R.id.comments).setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            getActivity().finish();
                            SubmissionsView.dataChanged(adapterPosition);
                        }
                    });
                } else {
                    rootView.findViewById(R.id.comments).setVisibility(View.GONE);
                }
            }
            return rootView;
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            Bundle bundle = this.getArguments();
            i = bundle.getInt("page", 0);
        }
    }

    public static void loadImage(final View rootView, Fragment f, String url, boolean single) {
        final SubsamplingScaleImageView image = rootView.findViewById(R.id.image);

        image.setMinimumDpi(70);
        image.setMinimumTileDpi(240);
        ImageView fakeImage = new ImageView(f.getActivity());
        final TextView size = rootView.findViewById(R.id.size);
        fakeImage.setLayoutParams(
                new LinearLayout.LayoutParams(image.getWidth(), image.getHeight()));
        fakeImage.setScaleType(ImageView.ScaleType.CENTER_CROP);
        ((Reddit) f.getActivity().getApplication()).getImageLoader()
                .displayImage(url, new ImageViewAware(fakeImage),
                        new DisplayImageOptions.Builder().resetViewBeforeLoading(true)
                                .cacheOnDisk(true)
                                .imageScaleType(single?ImageScaleType.NONE:ImageScaleType.NONE_SAFE)
                                .cacheInMemory(false)
                                .build(), new ImageLoadingListener() {

                            @Override
                            public void onLoadingStarted(String imageUri, View view) {
                                size.setVisibility(View.VISIBLE);
                            }

                            @Override
                            public void onLoadingFailed(String imageUri, View view,
                                    FailReason failReason) {
                                Log.v("Slide", "LOADING FAILED");

                            }

                            @Override
                            public void onLoadingComplete(String imageUri, View view,
                                    Bitmap loadedImage) {
                                size.setVisibility(View.GONE);
                                image.setImage(ImageSource.bitmap(loadedImage));
                                (rootView.findViewById(R.id.progress)).setVisibility(View.GONE);
                            }

                            @Override
                            public void onLoadingCancelled(String imageUri, View view) {
                                Log.v("Slide", "LOADING CANCELLED");

                            }
                        }, new ImageLoadingProgressListener() {
                            @Override
                            public void onProgressUpdate(String imageUri, View view, int current,
                                    int total) {
                                size.setText(FileUtil.readableFileSize(total));

                                ((ProgressBar) rootView.findViewById(R.id.progress)).setProgress(
                                        Math.round(100.0f * current / total));
                            }
                        });
    }

    private void showFirstDialog() {
        runOnUiThread(() ->
                DialogUtil.showFirstDialog(AlbumPager.this));
    }

    private void showErrorDialog() {
        runOnUiThread(() ->
                DialogUtil.showErrorDialog(AlbumPager.this));
    }

    @Override
    public void onFolderSelection(@NonNull FolderChooserDialogCreate dialog,
                                  @NonNull File folder, boolean isSaveToLocation) {
        Reddit.appRestart.edit().putString("imagelocation", folder.getAbsolutePath()).apply();
        Toast.makeText(this,
                getString(R.string.settings_set_image_location, folder.getAbsolutePath())
                        + folder.getAbsolutePath(), Toast.LENGTH_LONG).show();
    }

    @Override
    public void onFolderChooserDismissed(@NonNull FolderChooserDialogCreate dialog) {
    }
}
