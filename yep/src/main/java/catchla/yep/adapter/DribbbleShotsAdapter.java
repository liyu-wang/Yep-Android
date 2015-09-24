package catchla.yep.adapter;

import android.content.Context;
import android.support.v4.app.Fragment;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import java.util.List;

import catchla.yep.R;
import catchla.yep.model.DribbbleShot;
import catchla.yep.view.holder.DribbbleShotViewHolder;

/**
 * Created by mariotaku on 15/6/4.
 */
public class DribbbleShotsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private final Fragment mFragment;
    private final LayoutInflater mLayoutInflater;
    private List<DribbbleShot> mShots;

    public DribbbleShotsAdapter(final Fragment fragment, final Context context) {
        mFragment = fragment;
        mLayoutInflater = LayoutInflater.from(context);
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(final ViewGroup parent, final int viewType) {
        return new DribbbleShotViewHolder(mFragment, mLayoutInflater.inflate(R.layout.grid_item_gallery_provider_type, parent, false));
    }

    @Override
    public void onBindViewHolder(final RecyclerView.ViewHolder holder, final int position) {
        ((DribbbleShotViewHolder) holder).displayShot(mShots.get(position));
    }

    @Override
    public int getItemCount() {
        if (mShots == null) return 0;
        return mShots.size();
    }

    public void setData(List<DribbbleShot> shots) {
        mShots = shots;
        notifyDataSetChanged();
    }
}