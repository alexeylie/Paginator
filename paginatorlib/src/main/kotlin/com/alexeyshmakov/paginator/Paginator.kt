package com.alexeyshmakov.paginator

import android.animation.ArgbEvaluator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.support.annotation.ColorInt
import android.util.AttributeSet
import android.util.SparseArray
import android.view.View
import com.alexeyshmakov.paginatorlib.R
import java.lang.IllegalArgumentException

/**
 * @author Alexey Shmakov
 */
class Paginator : View {
    constructor(context: Context?) : this(context, null)
    constructor(context: Context?, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        val typedArray = context!!.obtainStyledAttributes(attrs, R.styleable.Paginator,
                defStyleAttr, R.style.Paginator)
        dotNormalSize = typedArray.getDimensionPixelSize(R.styleable.Paginator_dotSize, 0)
        dotSelectedSize = typedArray.getDimensionPixelSize(R.styleable.Paginator_dotSelectSize, 0)
        normalColor = typedArray.getColor(R.styleable.Paginator_dotNormalColor, 0)
        selectColor = typedArray.getColor(R.styleable.Paginator_dotSelectColor, 0)
        spaceBetweenDotsCenter =
                typedArray.getDimensionPixelSize(R.styleable.Paginator_dotSpace, 0) + dotNormalSize
        visibleDotCount = typedArray.getInt(R.styleable.Paginator_visibleDotCount, 0)
        minDotCount = typedArray.getInt(R.styleable.Paginator_minVisibleDotCount, 2)
        typedArray.recycle()
    }

    private var paint: Paint = Paint()
    private val colorEvaluator: ArgbEvaluator = ArgbEvaluator()
    private var paginatorAdapter: PaginatorAdapter<*>? = null
    private var attachRunnable: Runnable? = null

    private var itemCount: Int = 0
    private var spaceBetweenDotsCenter: Int = 0
    private var dotNormalSize: Int = 0
    private var dotSelectedSize: Int = 0
    private var normalColor: Int = 0
    private var selectColor: Int = 0
    private var visibleDotCount: Int = 0
    private var minDotCount: Int = 0
    private var infinityDotCount: Int = 0
    private var dotsScale: SparseArray<Float> = SparseArray()

    private var visibleFrameWidth: Int = 0
    private var visibleFramePosition: Float = 0F
    private var firstDotOffset: Float = 0F

    private var loop: Boolean = false
    private var dotsInited = false

    init {
        paint.isAntiAlias = true
    }

    private fun initDots(dotsCount: Int) {
        if (itemCount == dotsCount && dotsInited) {
            return
        }

        itemCount = dotsCount
        dotsInited = true

        if (dotsCount > minDotCount) {
            visibleFrameWidth = (visibleDotCount - 1) * spaceBetweenDotsCenter + dotSelectedSize
            firstDotOffset = if (loop && itemCount > visibleDotCount) 0F else (dotSelectedSize / 2).toFloat()
        }

        requestLayout()
        invalidate()
    }

    /**
     * Attaches to paginator adapter
     *
     * @param pager pager to attach
     * @param adapter which should setup this paginator to work with pager
     */
    fun <T> attach(pager: T, adapter: PaginatorAdapter<T>) {
        detach()
        adapter.attach(pager, this)
        paginatorAdapter = adapter

        attachRunnable = Runnable {
            itemCount = -1
            attach(pager, adapter)
        }
    }

    /**
     * Detaches paginator from pager.
     */
    fun detach() {
        if (paginatorAdapter != null) {
            paginatorAdapter?.detach()
            paginatorAdapter = null
            attachRunnable = null
        }
        dotsInited = false
    }

    /**
     * Sets dots count
     *
     * @param count dots count
     */
    fun setDotCount(count: Int) {
        initDots(count)
    }

    /**
     * Sets current selected position
     *
     * @param position current position
     */
    fun setCurrentPosition(position: Int) {
        if (position != 0 && (position < 0 || position >= itemCount)) {
            throw IndexOutOfBoundsException("Out of bounds 0..itemCount")
        }
        if (itemCount == 0) {
            return
        }
        adjustPositionInLayout(position, 0f)
        updateScaleInIdleState(position)
    }

    /**
     * Sets visible dots count
     *
     * @param count visible dots count
     */
    fun setVisibleDotCount(count: Int) {
        if (count % 2 == 0) {
            throw IllegalArgumentException("Count must be odd")
        }

        visibleDotCount = count
        infinityDotCount = visibleDotCount + 2

        requestLayout()
    }

    /**
     * @param looped true if pager is looped
     */
    fun setLooped(looped: Boolean) {
        loop = looped
        invalidate()
    }

    /**
     * @return not selected dots color
     */
    @ColorInt
    fun getDotColor(): Int {
        return normalColor
    }

    /**
     * Sets dots color
     *
     * @param color dots color
     */
    fun setDotColor(@ColorInt color: Int) {
        normalColor = color
        invalidate()
    }

    /**
     * @return the selected dot color
     */
    @ColorInt
    fun getSelectedDotColor(): Int {
        return selectColor
    }

    /**
     * Sets selected dots color
     *
     * @param color selected dot color
     */
    fun setSelectedDotColor(@ColorInt color: Int) {
        selectColor = color
        invalidate()
    }

    /**
     * This method must be called when pager scrolled
     *
     * @param page   index of the first page currently being displayed
     *               Page position+1 will be visible if offset is nonzero
     * @param offset Value from [0, 1) indicating the offset from the page at position
     */
    fun onPageScrolled(page: Int, offset: Float) {
        if (offset < 0 || offset > 1) {
            throw IllegalArgumentException("Offset must be [0, 1]")
        } else if (page < 0 || page != 0 && page >= itemCount) {
            throw IndexOutOfBoundsException("page must be [0, adapter.getItemCount())")
        }

        if (!loop || itemCount <= visibleDotCount && itemCount > 1) {
            dotsScale.clear()

            scaleDotByOffset(page, offset)

            if (page < itemCount - 1) {
                scaleDotByOffset(page + 1, 1 - offset)
            } else if (itemCount > 1) {
                scaleDotByOffset(0, 1 - offset)
            }

            invalidate()
        }
        adjustPositionInLayout(page, offset)
        invalidate()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val measureWidth = if (isInEditMode) {
            getDotsWidth(visibleDotCount)
        } else {
            if (itemCount >= visibleDotCount) visibleFrameWidth else getDotsWidth(itemCount)
        }


        val heightMode = MeasureSpec.getMode(heightMeasureSpec)
        val heightSize = MeasureSpec.getSize(heightMeasureSpec)

        val measureHeight = when (heightMode) {
            MeasureSpec.EXACTLY -> heightSize
            MeasureSpec.AT_MOST -> Math.min(dotSelectedSize, heightSize)
            MeasureSpec.UNSPECIFIED -> dotSelectedSize
            else -> dotSelectedSize
        }

        setMeasuredDimension(measureWidth, measureHeight)
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        val dotCount = getDotCount()
        if (dotCount < minDotCount) {
            return
        }

        val scaleDistance = (spaceBetweenDotsCenter + (dotSelectedSize - dotNormalSize) / 2) *
                SCALE_DISTANCE_FACTOR
        val smallScaleDistance: Float = (dotSelectedSize / 2).toFloat()
        val centerScaleDistance = CENTER_SCALE_DISTANCE_FACTOR * spaceBetweenDotsCenter

        val firstVisibleDotPos: Int = (visibleFramePosition - firstDotOffset).toInt() / spaceBetweenDotsCenter
        var lastVisibleDotPos: Int = firstVisibleDotPos + (visibleFramePosition + visibleFrameWidth
                - getDotOffsetAt(firstVisibleDotPos)).toInt() / spaceBetweenDotsCenter

        if (firstVisibleDotPos == 0 && lastVisibleDotPos + 1 > dotCount) {
            lastVisibleDotPos = dotCount - 1
        }

        for (i in firstVisibleDotPos..lastVisibleDotPos) {
            val dot = getDotOffsetAt(i)
            if (dot >= visibleFramePosition && dot < visibleFramePosition + visibleFrameWidth) {
                var diameter: Float
                val scale: Float

                //Normal scale
                scale = if (loop && itemCount > visibleDotCount) {
                    val frameCenter = visibleFramePosition + visibleFrameWidth / 2
                    if (dot >= frameCenter - centerScaleDistance && dot <= frameCenter) {
                        (dot - frameCenter + centerScaleDistance) / centerScaleDistance
                    } else if (dot > frameCenter && dot < frameCenter + centerScaleDistance) {
                        1 - (dot - frameCenter) / centerScaleDistance
                    } else {
                        0f
                    }
                } else {
                    getDotScaleAt(i)
                }
                diameter = dotNormalSize + (dotSelectedSize - dotNormalSize) * scale

                //Scale for dots at corners
                if (itemCount > visibleDotCount) {
                    val currentScaleDistance: Float = if (!loop && (i == 0 || i == dotCount - 1)) {
                        smallScaleDistance
                    } else {
                        scaleDistance
                    }

                    if (dot - visibleFramePosition < currentScaleDistance) {
                        val scaledDiameter = diameter * (dot - visibleFramePosition) / currentScaleDistance
                        if (scaledDiameter < diameter) {
                            diameter = scaledDiameter
                        }
                    } else if (dot - visibleFramePosition > width - currentScaleDistance) {
                        val scaledDiameter = diameter * (-dot + visibleFramePosition
                                + width.toFloat()) / currentScaleDistance
                        if (scaledDiameter < diameter) {
                            diameter = scaledDiameter
                        }
                    }
                }

                paint.color = calculateDotColor(scale)
                canvas?.drawCircle(dot - visibleFramePosition, (measuredHeight / 2).toFloat(),
                        diameter / 2, paint)
            }
        }
    }

    private fun getDotsWidth(count: Int): Int {
        return spaceBetweenDotsCenter * (count - 1) + dotSelectedSize
    }

    private fun adjustPositionInLayout(position: Int, offset: Float) {
        if (itemCount <= visibleDotCount) {
            visibleFramePosition = 0F
        } else if (!loop && itemCount > visibleDotCount) {
            val center = getDotOffsetAt(position) + spaceBetweenDotsCenter * offset
            visibleFramePosition = center - visibleFrameWidth / 2

            val firstCenteredDotIndex = visibleDotCount / 2
            val lastCenteredDot = getDotOffsetAt(getDotCount() - 1 - firstCenteredDotIndex)
            if (visibleFramePosition + visibleFrameWidth / 2 < getDotOffsetAt(firstCenteredDotIndex)) {
                visibleFramePosition = getDotOffsetAt(firstCenteredDotIndex) - visibleFrameWidth / 2
            } else if (visibleFramePosition + visibleFrameWidth / 2 > lastCenteredDot) {
                visibleFramePosition = lastCenteredDot - visibleFrameWidth / 2
            }
        } else {
            val center = getDotOffsetAt(infinityDotCount / 2) + spaceBetweenDotsCenter * offset
            visibleFramePosition = center - visibleFrameWidth / 2
        }
    }

    private fun getDotOffsetAt(index: Int): Float {
        return firstDotOffset + index * spaceBetweenDotsCenter
    }

    private fun getDotCount(): Int {
        return if (loop && itemCount > visibleDotCount) {
            infinityDotCount
        } else {
            itemCount
        }
    }

    private fun scaleDotByOffset(position: Int, offset: Float) {
        if (getDotCount() == 0) {
            return
        }
        setDotScaleAt(position, 1 - Math.abs(offset))
    }

    private fun setDotScaleAt(index: Int, scale: Float) {
        if (scale == 0f) {
            dotsScale.remove(index)
        } else {
            dotsScale.put(index, scale)
        }
    }

    private fun getDotScaleAt(index: Int): Float {
        val scale = dotsScale.get(index)
        return scale ?: 0f
    }

    @ColorInt
    private fun calculateDotColor(dotScale: Float): Int {
        return colorEvaluator.evaluate(dotScale, normalColor, selectColor) as Int
    }

    private fun updateScaleInIdleState(currentPos: Int) {
        if (!loop || itemCount < visibleDotCount) {
            dotsScale.clear()
            dotsScale.put(currentPos, 1f)
            invalidate()
        }
    }

    companion object {
        const val CENTER_SCALE_DISTANCE_FACTOR: Float = 6F / 7F
        const val SCALE_DISTANCE_FACTOR: Float = 0.7F
    }
}