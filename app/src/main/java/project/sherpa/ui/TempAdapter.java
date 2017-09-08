package project.sherpa.ui;

import android.databinding.DataBindingUtil;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.List;

import project.sherpa.R;
import project.sherpa.databinding.ListItemTempBinding;
import project.sherpa.models.Temp;
import project.sherpa.models.TempViewModel;
import project.sherpa.ui.adapters.interfaces.Hideable;

/**
 * Created by Alvin on 8/1/2017.
 */

public class TempAdapter extends RecyclerView.Adapter<TempAdapter.TempViewHolder> implements Hideable {
    List<Temp> mTempList;
    ClickHandler mClickHandler;
    private boolean hide;

    public TempAdapter(ClickHandler mClickHandler) {
        this.mClickHandler = mClickHandler;
    }

    @Override
    public TempViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        int layoutId = R.layout.list_item_temp;

        ListItemTempBinding binding = DataBindingUtil.inflate(inflater, layoutId, parent, false);
        return new TempViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(TempViewHolder holder, int position) {
        holder.bind(position);
    }

    @Override
    public int getItemCount() {

        if (hide) return 0;

        if (mTempList != null) {
            return mTempList.size();
        }
        return 0;
    }

    public void setTempList(List<Temp> tempList) {
        mTempList = tempList;

        notifyDataSetChanged();
    }

    public void addTemp(Temp temp) {
        mTempList.add(temp);
        notifyItemInserted(mTempList.size() - 1);
    }

    public int getPosition(Temp temp) {
        return mTempList.indexOf(temp);
    }

    public interface ClickHandler {
        void onClickTemp(Temp temp);
        void onLongClickTemp(Temp temp);
    }

    @Override
    public void hide() {
        hide = true;
        notifyDataSetChanged();
    }

    @Override
    public void show() {
        hide = false;
        notifyDataSetChanged();
    }

    class TempViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener, View.OnLongClickListener {
        ListItemTempBinding mBinding;

        public TempViewHolder(ListItemTempBinding binding) {
            super(binding.getRoot());

            mBinding = binding;

            mBinding.getRoot().setOnClickListener(this);
            mBinding.getRoot().setOnLongClickListener(this);
        }

        void bind(int position) {
            Temp temp = mTempList.get(position);
            TempViewModel vm = new TempViewModel(temp);

            mBinding.setVm(vm);
        }

        @Override
        public void onClick(View view) {
            int position = getAdapterPosition();
            Temp temp = mTempList.get(position);

            if (mClickHandler != null) {
                mClickHandler.onClickTemp(temp);
            }
        }

        @Override
        public boolean onLongClick(View view) {
            int position = getAdapterPosition();
            Temp temp = mTempList.get(position);

            if (mClickHandler != null) {
                mClickHandler.onLongClickTemp(temp);
                return true;
            }

            return false;
        }
    }
}
