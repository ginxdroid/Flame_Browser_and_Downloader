package com.ginxdroid.flamebrowseranddownloader.activities;

import androidx.recyclerview.widget.DiffUtil;

import java.util.ArrayList;

public class MyDiffUtilCallBack extends DiffUtil.Callback {

    final ArrayList<Integer> newList;
    final ArrayList<Integer> oldList;

    public MyDiffUtilCallBack(ArrayList<Integer> newList, ArrayList<Integer> oldList) {
        this.newList = newList;
        this.oldList = oldList;
    }

    @Override
    public int getOldListSize() {
        return oldList != null ? oldList.size() : 0;
    }

    @Override
    public int getNewListSize() {
        return newList != null ? newList.size() : 0;
    }

    @Override
    public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
        return newList.get(newItemPosition).equals(oldList.get(oldItemPosition));
    }

    @Override
    public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
        return newList.get(newItemPosition).equals(oldList.get(oldItemPosition));
    }
}
