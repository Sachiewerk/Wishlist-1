package com.company.wishlist.adapter;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.company.wishlist.R;
import com.company.wishlist.model.Wish;

import java.util.List;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * Created by vladstarikov on 08.01.16.
 */
public class WishListAdapter extends RecyclerView.Adapter<WishListAdapter.Holder> {

    private Context context;
    private List<Wish> wishes;

    private int selectedItem = -1;
    private  boolean editMode = false;

    public WishListAdapter(Context context, List<Wish> wishes) {
        this.context = context;
        this.wishes = wishes;
    }

    @Override
    public Holder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new Holder(LayoutInflater.from(parent.getContext()).inflate(R.layout.wish_list_item, parent, false));
    }

    @Override
    public void onBindViewHolder(Holder holder, int position) {
        holder.imageView.setImageResource(R.drawable.gift_icon);
        holder.textViewTitle.setText(wishes.get(position).getTitle());
        holder.textViewComment.setText(wishes.get(position).getComment());
        holder.setMode((selectedItem == position) ? (editMode ? holder.EDIT : holder.DETAIL) : holder.NORMAl);
    }

    @Override
    public int getItemCount() {
        return wishes.size();
    }

    public class Holder extends RecyclerView.ViewHolder implements View.OnClickListener{

        public final int NORMAl = 0;
        public final int DETAIL = 1;
        public final int EDIT = 2;

        //Normal mode layout
        @Bind(R.id.layout_main) ViewGroup layoutNormal;
        @Bind(R.id.image_view) ImageView imageView;
        @Bind(R.id.text_view_title) TextView textViewTitle;
        @Bind(R.id.text_view_comment) TextView textViewComment;

        //Footer mode layout
        @Bind(R.id.layout_footer) ViewGroup layoutFooter;
        @Bind(R.id.image_button_reserve) ImageButton imageButtonReserve;
        @Bind(R.id.image_button_edit) ImageButton imageButtonEdit;
        @Bind(R.id.image_button_delete) ImageButton imageButtonDelete;

        //Edit mode layout
        @Bind(R.id.layout_edit) ViewGroup layoutEdit;


        public Holder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
            itemView.setOnClickListener(this);
        }

        public void setMode(int mode) {
            layoutNormal.setVisibility(mode == NORMAl || mode == DETAIL ? View.VISIBLE : View.GONE);
            layoutFooter.setVisibility(mode == DETAIL || mode == EDIT ? View.VISIBLE : View.GONE);
            layoutEdit.setVisibility(mode == EDIT ? View.VISIBLE : View.GONE);
            textViewComment.setSingleLine(mode == NORMAl);
        }

        @Override
        public void onClick(View v) {
            int selectedItemOld = selectedItem;
            selectedItem = getAdapterPosition();
            editMode = false;
            if (selectedItem == selectedItemOld) selectedItem = -1;
            else notifyItemChanged(selectedItemOld);
            notifyItemChanged(getAdapterPosition());
        }

        @OnClick(R.id.image_button_reserve)
        public void onClickReserve() {
            Toast.makeText(context, "item " + getAdapterPosition() + " reserved", Toast.LENGTH_SHORT).show();
        }

        @OnClick(R.id.image_button_edit)
        public void onClickEdit() {
            editMode = true;
            notifyItemChanged(getAdapterPosition());
            Toast.makeText(context, "item " + getAdapterPosition() + " edit", Toast.LENGTH_SHORT).show();
        }

        @OnClick(R.id.image_button_delete)
        public void onClickDelete() {
            Toast.makeText(context, "item " + getAdapterPosition() + " deleted", Toast.LENGTH_SHORT).show();
        }
    }

}
