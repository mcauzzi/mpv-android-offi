package is.xyz.mpv;

import android.net.Uri;
import android.provider.DocumentsContract;
import android.view.View;

import androidx.annotation.NonNull;

import java.util.Collections;
import java.util.Set;

import is.xyz.filepicker.DocumentPickerFragment;

public class MPVDocumentPickerFragment extends DocumentPickerFragment {

    private Set<String> mLastPlayed = Collections.emptySet();
    private Set<String> mWatched = Collections.emptySet();

    public MPVDocumentPickerFragment(@NonNull Uri root) {
        super(root);
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
    protected boolean isLastPlayed(@NonNull Uri file) {
        return mLastPlayed.contains(file.toString());
    }

    @Override
    protected boolean isWatched(@NonNull Uri file) {
        return mWatched.contains(file.toString());
    }

    @Override
    protected boolean containsLastPlayed(@NonNull Uri dir) {
        // Document IDs are opaque, but many providers (e.g. local storage) use
        // path-like IDs, so we can at least try.
        String prefix;
        try {
            prefix = DocumentsContract.getDocumentId(dir) + "/";
        } catch (IllegalArgumentException e) {
            return false;
        }
        for (String s : mLastPlayed) {
            if (!s.startsWith("content://"))
                continue;
            Uri uri = Uri.parse(s);
            if (!dir.getAuthority().equals(uri.getAuthority()))
                continue;
            try {
                if (DocumentsContract.getDocumentId(uri).startsWith(prefix))
                    return true;
            } catch (IllegalArgumentException ignored) {
            }
        }
        return false;
    }

    @Override
    public void onClickCheckable(@NonNull View view, @NonNull FileViewHolder vh) {
        mListener.onDocumentPicked(vh.file, false);
    }

    @Override
    public boolean onLongClickCheckable(@NonNull View view, @NonNull DirViewHolder vh) {
        mListener.onDocumentPicked(vh.file, true);
        return true;
    }

    public boolean isBackTop() {
        return mCurrentPath.equals(getRoot());
    }
}
