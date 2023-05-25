package com.ginxdroid.flamebrowseranddownloader.activities;

import android.animation.ArgbEvaluator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.SparseArray;
import android.view.View;

import androidx.annotation.ColorInt;
import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.ginxdroid.flamebrowseranddownloader.R;

@Keep
public class RVIndicator extends View {
    private int infiniteDotCount;
    private final int dotNormalSize;
    private final int spaceBetweenDotCenters;
    private int visibleDotCount;
    private final int visibleDotThreshold;

    private float visibleFramePosition;
    private float visibleFrameWidth;

    private float firstDotOffset;
    private SparseArray<Float> dotScale;

    private int itemCount;

    private final Paint paint;
    private final ArgbEvaluator colorEvaluator = new ArgbEvaluator();

    @ColorInt
    private final int dotColor;

    @ColorInt
    private final int selectedDotColor;

    private final boolean looped;

    private Runnable attachRunnable;
    private PagerAttachInterface<?> attachInterface;

    private boolean dotCountInitialized;

    public RVIndicator(Context context) {
        this(context, null);
    }

    public RVIndicator(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, R.attr.RVIndicatorStyle);
    }

    public RVIndicator(Context context, @Nullable AttributeSet attrs, int defStyleAttr)
    {
        super(context,attrs,defStyleAttr);

        TypedArray attributes = context.obtainStyledAttributes(attrs, R.styleable.RVIndicator,defStyleAttr,R.style.RVIndicator);

        dotColor = attributes.getColor(R.styleable.RVIndicator_dotColor,0);
        selectedDotColor = attributes.getColor(R.styleable.RVIndicator_dotSelectedColor,dotColor);
        dotNormalSize = attributes.getDimensionPixelSize(R.styleable.RVIndicator_dotSize,0);
        spaceBetweenDotCenters = attributes.getDimensionPixelSize(R.styleable.RVIndicator_dotSpacing,0) + dotNormalSize;
        looped = attributes.getBoolean(R.styleable.RVIndicator_looped, false);
        int visibleDotCount = attributes.getInt(R.styleable.RVIndicator_visibleDotCount,0);
        setVisibleDotCount(visibleDotCount);
        visibleDotThreshold = 0;

        attributes.recycle();

        paint = new Paint();
        paint.setAntiAlias(true);

        if(isInEditMode())
        {
            setDotCount(visibleDotCount);
            onPageScrolled(visibleDotCount / 2, 0);
        }
    }


    public void setVisibleDotCount(int visibleDotCount)
    {
        this.visibleDotCount = visibleDotCount;
        this.infiniteDotCount = visibleDotCount + 2;

        if(attachRunnable != null)
        {
            reattach();
        }else {
            requestLayout();
        }
    }

    void attachToRecyclerView(@NonNull RecyclerView recyclerView)
    {
        attachToPager(recyclerView,new RecyclerViewAttach());
    }

    public <T> void attachToPager(@NonNull final T pager, final PagerAttachInterface<T> attachInterface)
    {
        detachFromPager();
        attachInterface.attachToPager(this,pager);
        this.attachInterface = attachInterface;

        attachRunnable = () -> {
            itemCount = -1;
            attachToPager(pager,attachInterface);
        };
    }

    public void detachFromPager()
    {
        if(attachInterface != null)
        {
            attachInterface.detachFromPager();
            attachInterface = null;
            attachRunnable = null;
        }

        dotCountInitialized = false;
    }

    public void reattach()
    {
        if(attachRunnable != null)
        {
            attachRunnable.run();
            invalidate();
        }
    }

    public void onPageScrolled(int page, float offset)
    {
        if(!looped || itemCount <= visibleDotCount && itemCount > 1)
        {
            dotScale.clear();

            scaleDotByOffset(page, offset);

            if(page < itemCount -1)
            {
                scaleDotByOffset(page + 1, 1-offset);
            } else if(itemCount > 1)
            {
                scaleDotByOffset(0, 1-offset);
            }

            invalidate();
        }

        adjustFramePosition(offset, page);
        invalidate();
    }

    public void setDotCount(int count)
    {
        initDots(count);
    }

    public void setCurrentPosition(int position)
    {
        if(itemCount == 0)
        {
            return;
        }

        adjustFramePosition(0, position);
        updateScaleInIdleState(position);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int measuredWidth;
        int measuredHeight;

        if(isInEditMode())
        {
            measuredWidth = (visibleDotCount - 1) * spaceBetweenDotCenters + dotNormalSize;
        }else {
            measuredWidth = itemCount >= visibleDotCount ?(int) visibleFrameWidth:(itemCount - 1) * spaceBetweenDotCenters +dotNormalSize;
        }

        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);

        int desiredHeight = dotNormalSize;

        switch (heightMode)
        {
            case MeasureSpec.EXACTLY:
                measuredHeight = heightSize;
                break;
                case MeasureSpec.AT_MOST:
                    measuredHeight = Math.min(desiredHeight, heightSize);
                    break;
            case MeasureSpec.UNSPECIFIED:
            default:
                measuredHeight = desiredHeight;
                break;
        }

        setMeasuredDimension(measuredWidth,measuredHeight);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        int dotCount = getDotCount();
        if(dotCount < visibleDotThreshold)
        {
            return;
        }

        float scaleDistance = (spaceBetweenDotCenters) * 0.7f;
        float smallScaleDistance = dotNormalSize / 2f;
        float centerScaleDistance = 6f / 7f * spaceBetweenDotCenters;

        int firstVisibleDotPos = (int) (visibleFramePosition - firstDotOffset) / spaceBetweenDotCenters;
        int lastVisibleDotPos = firstVisibleDotPos + (int) (visibleFramePosition + visibleFrameWidth - getDotOffsetAt(firstVisibleDotPos)) / spaceBetweenDotCenters;

        if(firstVisibleDotPos == 0 && lastVisibleDotPos +1 > dotCount)
        {
            lastVisibleDotPos = dotCount - 1;
        }

        for(int i = firstVisibleDotPos; i <= lastVisibleDotPos; i++)
        {
            float dot = getDotOffsetAt(i);
            if(dot >= visibleFramePosition && dot < visibleFramePosition + visibleFrameWidth)
            {
                float diameter;
                float scale;

                if(looped && itemCount > visibleDotCount)
                {
                    float frameCenter = visibleFramePosition + visibleFrameWidth / 2;
                    if(dot >= frameCenter - centerScaleDistance && dot <= frameCenter)
                    {
                        scale = (dot - frameCenter + centerScaleDistance) / centerScaleDistance;
                    } else if(dot > frameCenter && dot < frameCenter + centerScaleDistance)
                    {
                        scale = 1 - (dot - frameCenter);
                    } else {
                        scale = 0;
                    }
                } else {
                    scale = getDotScaleAt(i);
                }

                diameter = dotNormalSize;

                if(itemCount > visibleDotCount)
                {
                    float currentScaleDistance;
                    if(!looped && (i == 0 || i == dotCount - 1))
                    {
                        currentScaleDistance = smallScaleDistance;
                    } else {
                        currentScaleDistance = scaleDistance;
                    }

                    int size = getWidth();

                    if(dot - visibleFramePosition < currentScaleDistance)
                    {
                        float calculateDiameter = diameter * (dot - visibleFramePosition) / centerScaleDistance;
                        if(calculateDiameter <= dotNormalSize)
                        {
                            diameter = dotNormalSize;
                        } else if(calculateDiameter < diameter)
                        {
                            diameter = calculateDiameter;
                        }
                    } else if(dot - visibleFramePosition > size - currentScaleDistance)
                    {
                        float calculatedDiameter = diameter * (-dot + visibleFramePosition + size) / centerScaleDistance;
                        if(calculatedDiameter <= dotNormalSize)
                        {
                            diameter = dotNormalSize;
                        }else {
                            diameter = calculatedDiameter;
                        }
                    }
                }

                paint.setColor(calculateDotColor(scale));
                canvas.drawCircle(dot - visibleFramePosition, getMeasuredHeight() / 2f,
                        diameter/2, paint);

            }
        }
    }

    @ColorInt
    private int calculateDotColor(float dotScale)
    {
        return (Integer) colorEvaluator.evaluate(dotScale, dotColor, selectedDotColor);
    }

    private void updateScaleInIdleState(int currentPos)
    {
        if(!looped || itemCount < visibleDotCount)
        {
            dotScale.clear();
            dotScale.put(currentPos, 1f);
            invalidate();
        }
    }

    private void initDots(int itemCount)
    {
        if(this.itemCount == itemCount && dotCountInitialized)
        {
            return;
        }

        this.itemCount = itemCount;
        dotCountInitialized = true;
        dotScale = new SparseArray<>();

        if(itemCount < visibleDotThreshold)
        {
            requestLayout();
            invalidate();
            return;
        }

        firstDotOffset = looped && this.itemCount > visibleDotCount ? 0:dotNormalSize /2f;
        visibleFrameWidth = (visibleDotCount - 1) * spaceBetweenDotCenters + dotNormalSize;

        requestLayout();
        invalidate();
    }

    private int getDotCount()
    {
        if(looped && itemCount > visibleDotCount)
        {
            return infiniteDotCount;
        } else {
            return itemCount;
        }
    }

    private void adjustFramePosition(float offset, int pos)
    {
        if(itemCount <= visibleDotCount)
        {
            visibleFramePosition = 0;
        } else if(!looped)
        {
            float center = getDotOffsetAt(pos) + spaceBetweenDotCenters * offset;
            visibleFramePosition = center - visibleFrameWidth / 2;

            int firstCenteredDotIndex = visibleDotCount / 2;
            float lastCenteredDot = getDotOffsetAt(getDotCount() - 1 - firstCenteredDotIndex);
            if(visibleFramePosition + visibleFrameWidth / 2 < getDotOffsetAt(firstCenteredDotIndex)) {
                visibleFramePosition = getDotOffsetAt(firstCenteredDotIndex) - visibleFrameWidth / 2;
            } else if(visibleFramePosition + visibleFrameWidth / 2 > lastCenteredDot)
            {
                visibleFramePosition = lastCenteredDot - visibleFrameWidth / 2;
            }
        } else {
            float center = getDotOffsetAt(infiniteDotCount / 2) + spaceBetweenDotCenters * offset;
            visibleFramePosition = center - visibleFrameWidth / 2;
        }
    }

    private void scaleDotByOffset(int position, float offset)
    {
        if (dotScale == null || getDotCount() == 0)
        {
            return;
        }

        setDotScaleAt(position,1 - Math.abs(offset));
    }

    private float getDotOffsetAt(int index)
    {
        return firstDotOffset + index * spaceBetweenDotCenters;
    }

    private float getDotScaleAt(int index)
    {
        Float scale = dotScale.get(index);
        if(scale != null)
        {
            return scale;
        }

        return 0;
    }

    private void setDotScaleAt(int index, float scale)
    {
        if(scale == 0)
        {
            dotScale.remove(index);
        } else {
            dotScale.put(index,scale);
        }
    }

    private interface PagerAttachInterface<T>
    {
        void attachToPager(@NonNull RVIndicator indicator, T pager);

        void detachFromPager();
    }

    private static class RecyclerViewAttach implements PagerAttachInterface<RecyclerView>
    {
        private RVIndicator indicator;
        private RecyclerView recyclerView;
        private LinearLayoutManager layoutManager;
        private RecyclerView.Adapter<?> attachedAdapter;

        private RecyclerView.OnScrollListener scrollListener;
        private RecyclerView.AdapterDataObserver dataObserver;

        private final boolean centered;
        private final int currentPageOffset;
        private int measuredChildWidth;


        public RecyclerViewAttach()
        {
            currentPageOffset = 0;
            centered = true;
        }

        @Override
        public void attachToPager(@NonNull RVIndicator indicator, RecyclerView pager) {
            this.layoutManager = (LinearLayoutManager) pager.getLayoutManager();
            this.recyclerView = pager;
            this.attachedAdapter = pager.getAdapter();
            this.indicator = indicator;

            dataObserver = new RecyclerView.AdapterDataObserver() {
                @Override
                public void onChanged() {
                    indicator.setDotCount(attachedAdapter.getItemCount());
                    updateCurrentOffset();
                }

                @Override
                public void onItemRangeChanged(int positionStart, int itemCount) {
                    onChanged();
                }

                @Override
                public void onItemRangeChanged(int positionStart, int itemCount, @Nullable Object payload) {
                    onChanged();
                }

                @Override
                public void onItemRangeInserted(int positionStart, int itemCount) {
                    onChanged();
                }

                @Override
                public void onItemRangeRemoved(int positionStart, int itemCount) {
                    onChanged();
                }

                @Override
                public void onItemRangeMoved(int fromPosition, int toPosition, int itemCount) {
                    onChanged();
                }
            };

            //noinspection ConstantConditions
            attachedAdapter.registerAdapterDataObserver(dataObserver);
            indicator.setDotCount(attachedAdapter.getItemCount());
            updateCurrentOffset();

            scrollListener = new RecyclerView.OnScrollListener() {
                @Override
                public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                    if(newState == RecyclerView.SCROLL_STATE_IDLE)
                    {
                        int newPosition = findCompletelyVisiblePosition();

                        if(newPosition != RecyclerView.NO_POSITION)
                        {
                            indicator.setDotCount(attachedAdapter.getItemCount());

                            if(newPosition < attachedAdapter.getItemCount())
                            {
                                indicator.setCurrentPosition(newPosition);
                            }
                        }
                    }
                }

                @Override
                public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                    updateCurrentOffset();
                }
            };

            recyclerView.addOnScrollListener(scrollListener);
        }

        @Override
        public void detachFromPager() {
            attachedAdapter.unregisterAdapterDataObserver(dataObserver);
            recyclerView.removeOnScrollListener(scrollListener);
            measuredChildWidth = 0;
        }

        private void updateCurrentOffset()
        {
            final View firstView = findFirstVisibleView();

            if(firstView == null)
            {
                return;
            }

            int position = recyclerView.getChildAdapterPosition(firstView);

            if(position == RecyclerView.NO_POSITION)
            {
                return;
            }

            final int itemCount = attachedAdapter.getItemCount();

            //inCase there is an infinite scroll
            if(position >= itemCount && itemCount !=0)
            {
                position = position % itemCount;
            }

            float offset = (getCurrentFrameLeft() - firstView.getX()) / firstView.getMeasuredWidth();

            if(offset >= 0 && offset <= 1 && position < itemCount)
            {
                indicator.onPageScrolled(position, offset);
            }
        }

        private int findCompletelyVisiblePosition()
        {
            for(int i = 0;i < recyclerView.getChildCount(); i++)
            {
                View child = recyclerView.getChildAt(i);

                float position = child.getX();
                int size = child.getMeasuredWidth();
                float currentStart = getCurrentFrameLeft();
                float currentEnd = getCurrentFrameRight();

                if(position >= currentStart && position + size <= currentEnd)
                {
                    RecyclerView.ViewHolder holder = recyclerView.findContainingViewHolder(child);
                    if(holder != null && holder.getBindingAdapterPosition() != RecyclerView.NO_POSITION)
                    {
                        return holder.getBindingAdapterPosition();
                    }
                }
            }

            return RecyclerView.NO_POSITION;
        }



        @Nullable
        private View findFirstVisibleView()
        {
            int childCount = layoutManager.getChildCount();
            if( childCount == 0)
            {
                return null;
            }

            View closestChild = null;
            int firstVisibleChild = Integer.MAX_VALUE;

            for(int i = 0;i < childCount; i++)
            {
                final View child = layoutManager.getChildAt(i);

                //noinspection ConstantConditions
                int childStart = (int) child.getX();

                if(childStart + child.getMeasuredWidth() < firstVisibleChild && childStart + child.getMeasuredWidth() >= getCurrentFrameLeft())
                {
                    firstVisibleChild = childStart;
                    closestChild = child;
                }
            }
            return closestChild;
        }

        private float getCurrentFrameLeft()
        {
            if(centered){
                return (recyclerView.getMeasuredWidth() - getChildWidth()) / 2;
            } else {
                return currentPageOffset;
            }
        }

        private float getCurrentFrameRight()
        {
            if(centered){
                return (recyclerView.getMeasuredWidth() - getChildWidth()) / 2 + getChildWidth();
            } else {
                return currentPageOffset + getChildWidth();
            }
        }

        private float getChildWidth()
        {
            if(measuredChildWidth == 0)
            {
                for(int i = 0;i < recyclerView.getChildCount();i++)
                {
                    View child = recyclerView.getChildAt(i);
                    if(child.getMeasuredWidth() != 0)
                    {
                        measuredChildWidth = child.getMeasuredWidth();
                        return measuredChildWidth;
                    }
                }
            }

            return measuredChildWidth;
        }
    }
}
