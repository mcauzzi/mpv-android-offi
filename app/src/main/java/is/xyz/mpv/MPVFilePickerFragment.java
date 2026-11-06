package is.xyz.mpv;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import android.view.View;

import is.xyz.filepicker.FilePickerFragment;

import java.io.File;
import java.util.Collections;
import java.util.Set;

public class MPVFilePickerFragment extends FilePickerFragment {

    private File rootPath = new File("/");

    private Set<String> mLastPlayed = Collections.emptySet();
    private Set<String> mWatched = Collections.emptySet();

    MPVFilePickerFragment() {
        USE_ALL_FILE_ACCESS = BuildConfig.FLAVOR.equals("allstorage");
    }

    @Override
    public void onClickCheckable(@NonNull View v, @NonNull FileViewHolder vh) {
        mListener.onFilePicked(vh.file);
    }

    @Override
    public boolean onLongClickCheckable(@NonNull View v, @NonNull DirViewHolder vh) {
        mListener.onDirPicked(vh.file);
        return true;
    }

    @Override
    protected boolean reloadHistory() {
        Set<String> last = PlaybackHistory.getLastPlayed(requireContext());
        Set<String> watched = PlaybackHistory.getWatched(requireContext());
        if (last.equals(mLastPlayed) && watched.equals(mWatched))
            return false;
        mLastPlayed = last;
        mWatched = watched;
        return true;
    }

    @Override
    protected boolean isLastPlayed(@NonNull File file) {
        return mLastPlayed.contains(file.getAbsolutePath());
    }

    @Override
    protected boolean isWatched(@NonNull File file) {
        return mWatched.contains(file.getAbsolutePath());
    }

    @Override
    protected boolean containsLastPlayed(@NonNull File dir) {
        String prefix = dir.getAbsolutePath();
        if (!prefix.endsWith("/"))
            prefix += "/";
        for (String path : mLastPlayed) {
            if (path.startsWith(prefix))
                return true;
        }
        return false;
    }

    @NonNull
    @Override
    public File getRoot() {
        return rootPath;
    }

    public void setRoot(@NonNull File path) {
        rootPath = path;
    }

    public boolean isBackTop() {
        return mCurrentPath.equals(getRoot());
    }

    private @NonNull String makeRelative(@NonNull String path) {
        String head = getRoot().toString();
        if (path.equals(head))
            return "";
        if (!head.endsWith("/"))
            head += "/";
        return path.startsWith(head) ? path.substring(head.length()) : path;
    }

    @Override
    public void onChangePath(File file) {
        ActionBar bar = ((AppCompatActivity)getActivity()).getSupportActionBar();
        if (file != null && bar != null)
            bar.setSubtitle(makeRelative(file.getPath()));
    }
}
