package fr.free.nrw.commons.depictions;

import android.widget.ListAdapter;

import java.util.List;

import fr.free.nrw.commons.BasePresenter;
import fr.free.nrw.commons.Media;

public interface DepictedImagesContract {

    interface View {

        void handleNoInternet();

        void initErrorView();

        void setAdapter(List<Media> mediaList);

        void handleLabelforImage(String s, int position);

        void showSnackBar();

        void viewPagerNotifyDataSetChanged();

        void progressBarVisible(Boolean value);

        ListAdapter getAdapter();

        void addItemsToAdapter(List<Media> media);

        void setLoadingStatus(Boolean value);

        void handleSuccess(List<Media> collection);

    }

    interface UserActionListener extends BasePresenter<View> {

        void initList(String entityId);

        void fetchMoreImages();

        void replaceTitlesWithCaptions(String title, int position);

        void addItemsToQueryList(List<Media> collection);
    }
}