// Copyright 2018-2019 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// http://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.simpleadsdemo;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.mopub.common.MoPubReward;
import com.mopub.mobileads.MoPubErrorCode;
import com.mopub.mobileads.MoPubRewardedVideoListener;
import com.mopub.mobileads.MoPubRewardedVideoManager.RequestParameters;
import com.mopub.mobileads.MoPubRewardedVideos;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import static com.mopub.simpleadsdemo.Utils.hideSoftKeyboard;
import static com.mopub.simpleadsdemo.Utils.logToast;

public class RewardedVideoDetailFragment extends Fragment implements MoPubRewardedVideoListener {

    @Nullable private Button mShowButton;
    @Nullable private String mAdUnitId;
    @Nullable private Map<String, MoPubReward> mMoPubRewardsMap;
    @Nullable private MoPubReward mSelectedReward;
    @Nullable private CallbacksAdapter mCallbacksAdapter;

    private enum RewardedCallbacks {
        LOADED("onRewardedVideoLoadSuccess"),
        FAILED("onRewardedVideoLoadFailed"),
        STARTED("onRewardedVideoStarted"),
        PLAY_ERROR("onRewardedVideoPlaybackError"),
        CLICKED("onRewardedVideoClicked"),
        CLOSED("onRewardedVideoClosed"),
        COMPLETED("onRewardedVideoCompleted");

        RewardedCallbacks(@NonNull final String name) {
            this.name = name;
        }

        @NonNull
        private final String name;

        @Override
        @NonNull
        public String toString() {
            return name;
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        final MoPubSampleAdUnit adConfiguration =
                MoPubSampleAdUnit.fromBundle(getArguments());
        final View view = inflater.inflate(R.layout.interstitial_detail_fragment, container, false);
        final DetailFragmentViewHolder views = DetailFragmentViewHolder.fromView(view);
        views.mKeywordsField.setText(getArguments().getString(MoPubListFragment.KEYWORDS_KEY, ""));
        views.mUserDataKeywordsField.setText(getArguments().getString(MoPubListFragment.USER_DATA_KEYWORDS_KEY, ""));
        hideSoftKeyboard(views.mKeywordsField);
        hideSoftKeyboard(views.mUserDataKeywordsField);

        MoPubRewardedVideos.setRewardedVideoListener(this);

        mAdUnitId = adConfiguration.getAdUnitId();
        mMoPubRewardsMap = new HashMap<>();

        views.mDescriptionView.setText(adConfiguration.getDescription());
        views.mAdUnitIdView.setText(mAdUnitId);
        views.mLoadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mAdUnitId == null) {
                    return;
                }
                if (mCallbacksAdapter != null) {
                    mCallbacksAdapter.generateCallbackList(RewardedCallbacks.class);
                }
                MoPubRewardedVideos.loadRewardedVideo(mAdUnitId,
                        new RequestParameters(views.mKeywordsField.getText().toString(), views.mUserDataKeywordsField.getText().toString(),null,
                                "sample_app_customer_id"));
                if (mShowButton != null) {
                    mShowButton.setEnabled(false);
                }
            }
        });
        mShowButton = views.mShowButton;
        mShowButton.setEnabled(false);
        mShowButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mAdUnitId == null) {
                    return;
                }

                final String customData = (views.mCustomDataField != null)
                        ? views.mCustomDataField.getText().toString()
                        : null;

                MoPubRewardedVideos.showRewardedVideo(mAdUnitId, customData);
            }
        });
        if (views.mCustomDataField != null) {
            views.mCustomDataField.setVisibility(View.VISIBLE);
        }

        final RecyclerView callbacksView = view.findViewById(R.id.callbacks_recycler_view);
        final Context context = getContext();
        if (callbacksView != null && context != null) {
            callbacksView.setLayoutManager(new LinearLayoutManager(context));
            mCallbacksAdapter = new CallbacksAdapter(context);
            mCallbacksAdapter.generateCallbackList(RewardedCallbacks.class);
            callbacksView.setAdapter(mCallbacksAdapter);
        }

        return view;
    }

    @Override
    public void onDestroyView() {
        MoPubRewardedVideos.setRewardedVideoListener(null);
        super.onDestroyView();
    }

    // MoPubRewardedVideoListener implementation
    @Override
    public void onRewardedVideoLoadSuccess(@NonNull final String adUnitId) {
        if (adUnitId.equals(mAdUnitId)) {
            if (mShowButton != null) {
                mShowButton.setEnabled(true);
            }
            if (mCallbacksAdapter == null) {
                logToast(getActivity(), "Rewarded video loaded.");
            } else {
                mCallbacksAdapter.notifyCallbackCalled(RewardedCallbacks.LOADED.toString());
            }

            Set<MoPubReward> availableRewards = MoPubRewardedVideos.getAvailableRewards(mAdUnitId);

            // If there are more than one reward available, pop up alert dialog for reward selection
            if (availableRewards.size() > 1) {
                final SelectRewardDialogFragment selectRewardDialogFragment
                        = SelectRewardDialogFragment.newInstance();

                // The user must select a reward from the dialog
                selectRewardDialogFragment.setCancelable(false);

                // Reset rewards mapping and selected reward
                mMoPubRewardsMap.clear();
                mSelectedReward = null;

                // Initialize mapping between reward string and reward instance
                for (MoPubReward reward : availableRewards) {
                    mMoPubRewardsMap.put(reward.getAmount() + " " + reward.getLabel(), reward);
                }

                selectRewardDialogFragment.loadRewards(mMoPubRewardsMap.keySet()
                        .toArray(new String[mMoPubRewardsMap.size()]));
                selectRewardDialogFragment.setTargetFragment(this, 0);
                selectRewardDialogFragment.show(getActivity().getSupportFragmentManager(),
                        "selectReward");
            }
        }
    }

    @Override
    public void onRewardedVideoLoadFailure(@NonNull final String adUnitId, @NonNull final MoPubErrorCode errorCode) {
        if (adUnitId.equals(mAdUnitId)) {
            if (mShowButton != null) {
                mShowButton.setEnabled(false);
            }
            if (mCallbacksAdapter == null) {
                logToast(getActivity(), String.format(Locale.US, "Rewarded video failed to load: %s",
                        errorCode.toString()));
                return;
            }
            mCallbacksAdapter.notifyCallbackCalled(RewardedCallbacks.FAILED.toString(),
                    errorCode.toString());
        }
    }

    @Override
    public void onRewardedVideoStarted(@NonNull final String adUnitId) {
        if (adUnitId.equals(mAdUnitId)) {
            if (mShowButton != null) {
                mShowButton.setEnabled(false);
            }
            if (mCallbacksAdapter == null) {
                logToast(getActivity(), "Rewarded video started.");
                return;
            }
            mCallbacksAdapter.notifyCallbackCalled(RewardedCallbacks.STARTED.toString());
        }
    }

    @Override
    public void onRewardedVideoPlaybackError(@NonNull final String adUnitId, @NonNull final MoPubErrorCode errorCode) {
        if (adUnitId.equals(mAdUnitId)) {
            if (mShowButton != null) {
                mShowButton.setEnabled(false);
            }
            if (mCallbacksAdapter == null) {
                logToast(getActivity(), String.format(Locale.US, "Rewarded video playback error: %s",
                        errorCode.toString()));
                return;
            }
            mCallbacksAdapter.notifyCallbackCalled(RewardedCallbacks.PLAY_ERROR.toString(),
                    errorCode.toString());
        }
    }

    @Override
    public void onRewardedVideoClicked(@NonNull final String adUnitId) {
        if (adUnitId.equals(mAdUnitId)) {
            if (mCallbacksAdapter == null) {
                logToast(getActivity(), "Rewarded video clicked.");
                return;
            }
            mCallbacksAdapter.notifyCallbackCalled(RewardedCallbacks.CLICKED.toString());
        }
    }

    @Override
    public void onRewardedVideoClosed(@NonNull final String adUnitId) {
        if (adUnitId.equals(mAdUnitId)) {
            if (mShowButton != null) {
                mShowButton.setEnabled(false);
            }
            if (mCallbacksAdapter == null) {
                logToast(getActivity(), "Rewarded video closed.");
                return;
            }
            mCallbacksAdapter.notifyCallbackCalled(RewardedCallbacks.CLOSED.toString());
        }
    }

    @Override
    public void onRewardedVideoCompleted(@NonNull final Set<String> adUnitIds,
            @NonNull final MoPubReward reward) {
        if (adUnitIds.contains(mAdUnitId)) {
            final String message = String.format(Locale.US,
                    "Rewarded video completed with reward  \"%d %s\"",
                    reward.getAmount(),
                    reward.getLabel());
            if (mCallbacksAdapter == null) {
                logToast(getActivity(), message);
                return;
            }
            mCallbacksAdapter.notifyCallbackCalled(RewardedCallbacks.COMPLETED.toString(), message);
        }
    }

    public void selectReward(@NonNull String selectedReward) {
        mSelectedReward = mMoPubRewardsMap.get(selectedReward);
        MoPubRewardedVideos.selectReward(mAdUnitId, mSelectedReward);
    }

    public static class SelectRewardDialogFragment extends DialogFragment {
        @NonNull private String[] mRewards;
        @NonNull private String mSelectedReward;

        public static SelectRewardDialogFragment newInstance() {
            return new SelectRewardDialogFragment();
        }

        public void loadRewards(@NonNull String[] rewards) {
            mRewards = rewards;
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            AlertDialog dialog = new AlertDialog.Builder(getActivity())
                    .setTitle("Select a reward")
                    .setSingleChoiceItems(mRewards, -1, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            mSelectedReward = mRewards[which];
                        }
                    })
                    .setPositiveButton("Select", null)
                    .create();

            // Overriding onShow() of dialog's OnShowListener() and onClick() of the Select button's
            // OnClickListener() to prevent the dialog from dismissing upon any button click without
            // selecting an item first.
            dialog.setOnShowListener(new DialogInterface.OnShowListener() {
                @Override
                public void onShow(DialogInterface dialog) {
                    Button selectButton = ((AlertDialog) dialog).getButton(AlertDialog.BUTTON_POSITIVE);
                    selectButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            if (mSelectedReward != null) {
                                ((RewardedVideoDetailFragment) getTargetFragment())
                                        .selectReward(mSelectedReward);
                                dismiss();
                            }
                        }
                    });
                }
            });

            return dialog;
        }
    }
}
