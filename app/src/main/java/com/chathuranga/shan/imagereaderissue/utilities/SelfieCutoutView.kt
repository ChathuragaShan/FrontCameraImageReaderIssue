package com.chathuranga.shan.imagereaderissue.utilities

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.TypedArray
import android.graphics.*
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import androidx.vectordrawable.graphics.drawable.VectorDrawableCompat
import com.chathuranga.shan.imagereaderissue.R


/**
 * This class is responsible for creating view which has a transparent inner circle area
 * which use to show camera preview through
 */
class SelfieCutoutView(context: Context, attrs: AttributeSet) : View(context, attrs) {

    private var mainWidth = 0
    private var mainHeight = 0

    //margins camera cutout to parent layout
    private var customMargin = 0f

    private var drawableMarginTop = 0f

    // camera cutout radius
    private var circleRadius = 0f

    // handler stroke color
    private var dotedCircleColor = 0

    //view out side fill color
    private var outSideBackgroundColor = 0

    private var backgroundDrawableColor = 0

    private var drawableHeight = 0f

    //background drawable
    private var drawable: Drawable? = null

    private val circlePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val fakeBackgroundPaint =  Paint(Paint.ANTI_ALIAS_FLAG)

    init {

        val attributeSet: TypedArray =
            context.obtainStyledAttributes(attrs, R.styleable.SelfieCutoutView)

        outSideBackgroundColor = attributeSet
            .getColor(
                R.styleable.SelfieCutoutView_backgroundColor,
                ContextCompat.getColor(context, R.color.gray)
            )

        backgroundDrawableColor = attributeSet
            .getColor(
                R.styleable.SelfieCutoutView_backgroundDrawableColor,
                ContextCompat.getColor(context, R.color.gray)
            )

        customMargin = attributeSet
            .getDimension(R.styleable.SelfieCutoutView_cutoutMargin, 0f)
        circleRadius = attributeSet
            .getDimension(R.styleable.SelfieCutoutView_circleRadius, 0f)
        drawableHeight = attributeSet
            .getDimension(R.styleable.SelfieCutoutView_backgroundDrawableHeight, 0f)
        drawableMarginTop = attributeSet
            .getDimension(R.styleable.SelfieCutoutView_backgroundDrawableMarginTop,0f)

        dotedCircleColor =  attributeSet
            .getColor(
                R.styleable.SelfieCutoutView_dotedCircleColor,
                ContextCompat.getColor(context, R.color.white)
            )

        val drawableId = attributeSet
            .getResourceId(R.styleable.SelfieCutoutView_backgroundDrawable, 0)

        if (drawableId != 0) {
            drawable = VectorDrawableCompat.create(resources, drawableId, null)
        }

        circlePaint.style = Paint.Style.FILL
        circlePaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)

        fakeBackgroundPaint.style = Paint.Style.FILL
        fakeBackgroundPaint.color = backgroundDrawableColor

        attributeSet.recycle()
    }

    @SuppressLint("DrawAllocation")
    override fun onDraw(canvas: Canvas?) {

        super.onDraw(canvas)
        val bitmap = drawBackgroundBitmap()

        canvas?.drawBitmap(bitmap, 0f, 0f, null)

        if(drawable != null){

            val drawableTop = ((height) / 2 - (circleRadius).toInt()) + drawableMarginTop.toInt()
            val drawableBottom = drawableTop + drawableHeight.toInt()

            //Apply human figure vector to the canvas
            drawable?.setBounds(0, drawableTop, width, drawableBottom)
            drawable?.draw(canvas!!)

            //Fill with view to show as background image continue
            val continueBackground = Rect(0,drawableBottom,width,height)
            canvas?.drawRect(continueBackground,fakeBackgroundPaint)
        }

        drawCircleCutout(canvas)
    }

    private fun drawBackgroundBitmap(): Bitmap {

        mainWidth = width
        mainHeight = height

        //Create Bitmap with background color
        val bitmap = Bitmap.createBitmap(mainWidth, mainHeight, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(Color.TRANSPARENT)

        val canvasBitmap = Canvas(bitmap)
        canvasBitmap.drawColor(outSideBackgroundColor)

        return bitmap
    }

    private fun drawCircleCutout(canvas: Canvas?) {

        canvas?.drawCircle(
            (mainWidth / 2).toFloat(),
            (mainHeight / 2).toFloat(),
            circleRadius,
            circlePaint
        )

        val handlerPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        handlerPaint.style = Paint.Style.STROKE
        handlerPaint.color = dotedCircleColor
        handlerPaint.strokeWidth = 8f
        handlerPaint.pathEffect = DashPathEffect(floatArrayOf(20f, 20f), 0.5.toFloat())

        canvas?.drawCircle(
            (mainWidth / 2).toFloat(),
            (mainHeight / 2).toFloat(),
            circleRadius + 20,
            handlerPaint
        )
    }

}