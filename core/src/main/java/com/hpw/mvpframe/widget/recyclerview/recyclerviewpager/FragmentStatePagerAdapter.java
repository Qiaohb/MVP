package com.hpw.mvpframe.widget.recyclerview.recyclerviewpager;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.hpw.mvpframe.R;
import com.hpw.mvpframe.utils.LogUtil;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

/**
 * Implementation of {@link android.support.v4.view.PagerAdapter} that
 * uses a {@link android.support.v4.app.Fragment} to manage each page. This class also handles
 * saving and restoring of fragment's state.
 * <p/>
 * <p>This version of the pager is more useful when there are a large number
 * of pages, working more like a list view.  When pages are not visible to
 * the user, their entire fragment may be destroyed, only keeping the saved
 * state of that fragment.  This allows the pager to hold on to much less
 * memory associated with each visited page as compared to
 * {@link android.support.v4.app.FragmentPagerAdapter} at the cost of potentially more overhead when
 * switching between pages.
 * <p/>
 * <p>When using FragmentPagerAdapter the host ViewPager must have a
 * valid ID set.</p>
 * <p/>
 * <p>Subclasses only need to implement {@link #getItem(int, Fragment.SavedState)}
 * and {@link #getItemCount()} to have a working adapter.
 * <p>Warning:The fragment container id will be a simple sequence like [1,2,3....];
 * If you don't like this,you should use custom ContainerIdGenerator by  {@link #setContainerIdGenerator(IContainerIdGenerator)}
 * </p>
 */
@TargetApi(12)
public abstract class FragmentStatePagerAdapter extends RecyclerView.Adapter<FragmentStatePagerAdapter.FragmentViewHolder> {
    private static final String TAG = "FragmentStatePagerAdapter";

    private final FragmentManager mFragmentManager;
    private FragmentTransaction mCurTransaction = null;
    private SparseArray<Fragment.SavedState> mStates = new SparseArray<>();
    private Set<Integer> mIds = new HashSet<>();
    private IContainerIdGenerator mContainerIdGenerator = new IContainerIdGenerator() {
        private Random mRandom = new Random();

        @Override
        public int genId(Set<Integer> idContainer) {
            return Math.abs(mRandom.nextInt());
        }
    };

    public FragmentStatePagerAdapter(FragmentManager fm) {
        mFragmentManager = fm;
    }

    /**
     * set custom idGenerator
     */
    public void setContainerIdGenerator(@NonNull IContainerIdGenerator idGenerator) {
        mContainerIdGenerator = idGenerator;
    }

    @Override
    public void onViewRecycled(FragmentViewHolder holder) {
        if (mCurTransaction == null) {
            mCurTransaction = mFragmentManager.beginTransaction();
        }
        LogUtil.v(TAG, "Removing item #");
        int tagId = genTagId(holder.getAdapterPosition());
        Fragment f = mFragmentManager.findFragmentByTag(tagId + "");
        if (f != null) {
            LogUtil.v(TAG, "Removing fragment #");
            mStates.put(tagId, mFragmentManager.saveFragmentInstanceState(f));
            mCurTransaction.remove(f);
            mCurTransaction.commitAllowingStateLoss();
            mCurTransaction = null;
            mFragmentManager.executePendingTransactions();
        }
        if (holder.itemView instanceof ViewGroup) {
            ((ViewGroup) holder.itemView).removeAllViews();
        }
        super.onViewRecycled(holder);
    }

    @Override
    public final FragmentViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.rvp_fragment_container, parent, false);
        int id = mContainerIdGenerator.genId(mIds);
        if (parent.getContext() instanceof Activity) {
            while (((Activity) parent.getContext()).getWindow().getDecorView().findViewById(id) != null) {
                id = mContainerIdGenerator.genId(mIds);
            }
        }
        view.findViewById(R.id.rvp_fragment_container).setId(id);
        mIds.add(id);
        return new FragmentViewHolder(view);
    }

    @Override
    public final void onBindViewHolder(final FragmentViewHolder holder, int position) {
        // do nothing
    }

    protected int genTagId(int position) {
        // itemId must not be zero
        long itemId = getItemId(position);
        if (itemId == RecyclerView.NO_ID) {
            return position + 1;
        } else {
            return (int) itemId;
        }
    }

    /**
     * Return the Fragment associated with a specified position.
     */
    public abstract Fragment getItem(int position, Fragment.SavedState savedState);

    public abstract void onDestroyItem(int position, Fragment fragment);

    public class FragmentViewHolder extends RecyclerView.ViewHolder implements View.OnAttachStateChangeListener {

        public FragmentViewHolder(View itemView) {
            super(itemView);
            itemView.addOnAttachStateChangeListener(this);
        }

        @Override
        public void onViewAttachedToWindow(View v) {
            if (mCurTransaction == null) {
                mCurTransaction = mFragmentManager.beginTransaction();
            }
            final int tagId = genTagId(getLayoutPosition());
            final Fragment fragmentInAdapter = getItem(getLayoutPosition(), mStates.get(tagId));
            if (fragmentInAdapter != null) {
                mCurTransaction.replace(itemView.getId(), fragmentInAdapter, tagId + "");
                mCurTransaction.commitAllowingStateLoss();
                mCurTransaction = null;
                mFragmentManager.executePendingTransactions();
            }
        }

        @Override
        public void onViewDetachedFromWindow(View v) {
            LogUtil.v(TAG, "Removing fragment #");
            final int tagId = genTagId(getLayoutPosition());
            Fragment frag = mFragmentManager.findFragmentByTag(tagId + "");
            if (frag == null) {
                return;
            }
            if (mCurTransaction == null) {
                mCurTransaction = mFragmentManager.beginTransaction();
            }
            mStates.put(tagId, mFragmentManager.saveFragmentInstanceState(frag));
            mCurTransaction.remove(frag);
            mCurTransaction.commitAllowingStateLoss();
            mCurTransaction = null;
            mFragmentManager.executePendingTransactions();
            onDestroyItem(getLayoutPosition(), frag);
        }
    }

    public interface IContainerIdGenerator {
        int genId(Set<Integer> idContainer);
    }
}
