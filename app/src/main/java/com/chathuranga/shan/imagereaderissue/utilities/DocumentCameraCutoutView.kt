package com.chathuranga.shan.imagereaderissue.utilities

import android.content.Context
import android.content.res.TypedArray
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.chathuranga.shan.imagereaderissue.R

/**
 * This class is responsible for creating view which has a transparent inner rectangle area
 * which use to show camera preview through
 */
class DocumentCameraCutoutView(context: Context, attrs: AttributeSet) : View(context, attrs) {

    private var mainWidth = 0
    private var mainHeight = 0

    //view out side fill color
    private var outSideBackgroundColor = 0

    // handler stroke color
    private var handlerColor = 0

    //margins from camera cutout to parent layout
    private var leftMargin = 0f
    private var rightMargin = 0f
    private var topMargin = 0f
    private var bottomMargin = 0f

    //Camera cutout conner raius
    private var cornerRadius = 20f

    //Margin to outline handler from camera cut out
    private var handlerMarginFromCameraCutout = 6

    //Optimal diagonal distance between two points on the cutout rectangle stroke to draw the arc
    private var arcPointDiagonalDistance = 50

    init {

        val attributeSet: TypedArray =
            context.obtainStyledAttributes(attrs, R.styleable.DocumentCameraCutoutView)

        leftMargin = attributeSet
            .getDimension(R.styleable.DocumentCameraCutoutView_cutoutLeftMargin, 0f)
        rightMargin = attributeSet
            .getDimension(R.styleable.DocumentCameraCutoutView_cutoutRightMargin, 0f)
        topMargin = attributeSet
            .getDimension(R.styleable.DocumentCameraCutoutView_cutoutTopMargin, 0f)
        bottomMargin = attributeSet
            .getDimension(R.styleable.DocumentCameraCutoutView_cutoutBottomMargin, 0f)
        cornerRadius = attributeSet
            .getDimension(R.styleable.DocumentCameraCutoutView_connerRadius, 20f)

        outSideBackgroundColor = attributeSet
            .getColor(
                R.styleable.DocumentCameraCutoutView_backgroundColor,
                ContextCompat.getColor(context, R.color.gray)
            )

        handlerColor =  attributeSet
            .getColor(
                R.styleable.DocumentCameraCutoutView_handlerColor,
                ContextCompat.getColor(context, R.color.white)
            )

        attributeSet.recycle()

    }

    override fun onDraw(canvas: Canvas?) {

        super.onDraw(canvas)
        val bitmap = bitmapDraw()
        canvas?.drawBitmap(bitmap, 0f, 0f, null)
    }


    private fun bitmapDraw(): Bitmap {

        mainWidth = width
        mainHeight = height

        //Create Bitmap with background color
        val bitmap = Bitmap.createBitmap(mainWidth, mainHeight, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(Color.TRANSPARENT)

        val canvasBitmap = Canvas(bitmap)
        canvasBitmap.drawColor(outSideBackgroundColor)

        // Paint rectangle with transparent middle
        val rectanglePaint = Paint(Paint.ANTI_ALIAS_FLAG)
        rectanglePaint.style = Paint.Style.FILL
        rectanglePaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)

        val transparentRectangleRight = mainWidth - leftMargin // X2
        val transparentRectangleBottom = (mainHeight - bottomMargin).toFloat() //Y1
        val transparentRectangleLeft = mainWidth - (mainWidth - leftMargin) //X1
        val transparentRectangleTop = mainHeight - (mainHeight - topMargin).toFloat() //Y2

        val cutoutRectangle = RectF(
            transparentRectangleLeft,
            transparentRectangleTop,
            transparentRectangleRight,
            transparentRectangleBottom
        )

        canvasBitmap.drawRoundRect(cutoutRectangle, cornerRadius, cornerRadius, rectanglePaint)

        val handlerPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        handlerPaint.style = Paint.Style.STROKE
        handlerPaint.color = handlerColor
        handlerPaint.strokeWidth = 8f

        //------------ Paint Top Left Conner handler -------------------//

        val ovalTopLeft = RectF(
            transparentRectangleLeft - handlerMarginFromCameraCutout,
            transparentRectangleTop - handlerMarginFromCameraCutout,
            transparentRectangleLeft + arcPointDiagonalDistance,
            transparentRectangleTop + arcPointDiagonalDistance
        )

        val topLeftPath = Path()

        topLeftPath.arcTo(ovalTopLeft, 180f, 90f, true)

        topLeftPath.moveTo(transparentRectangleLeft + 21,transparentRectangleTop - handlerMarginFromCameraCutout)
        topLeftPath.lineTo(transparentRectangleLeft + 100,transparentRectangleTop - handlerMarginFromCameraCutout)

        topLeftPath.moveTo(transparentRectangleLeft - handlerMarginFromCameraCutout,transparentRectangleTop + 21)
        topLeftPath.lineTo(transparentRectangleLeft - handlerMarginFromCameraCutout,transparentRectangleTop + 100)

        canvasBitmap.drawPath(topLeftPath,handlerPaint)

        //------------ Paint Top Right Conner handler -------------------//

        val ovalTopRight = RectF(
            transparentRectangleRight - arcPointDiagonalDistance,
            transparentRectangleTop - handlerMarginFromCameraCutout,
            transparentRectangleRight + handlerMarginFromCameraCutout,
            transparentRectangleTop + arcPointDiagonalDistance
        )

        val topRightPath = Path()
        topRightPath.arcTo(ovalTopRight, 270f, 90f, true)

        topRightPath.moveTo(transparentRectangleRight - 21,transparentRectangleTop - handlerMarginFromCameraCutout)
        topRightPath.lineTo(transparentRectangleRight - 100,transparentRectangleTop - handlerMarginFromCameraCutout)

        topRightPath.moveTo(transparentRectangleRight + handlerMarginFromCameraCutout,transparentRectangleTop + 21)
        topRightPath.lineTo(transparentRectangleRight + handlerMarginFromCameraCutout,transparentRectangleTop + 100)

        canvasBitmap.drawPath(topRightPath,handlerPaint)

        //------------ Paint Bottom Left Conner handler -------------------//

        val ovalTopBottom = RectF(
            transparentRectangleLeft - handlerMarginFromCameraCutout,
            transparentRectangleBottom - arcPointDiagonalDistance,
            transparentRectangleLeft + arcPointDiagonalDistance,
            transparentRectangleBottom + handlerMarginFromCameraCutout
        )

        val bottomLeftPath = Path()
        bottomLeftPath.arcTo(ovalTopBottom, 180f, -90f, true)

        bottomLeftPath.moveTo(transparentRectangleLeft + 21,transparentRectangleBottom + handlerMarginFromCameraCutout)
        bottomLeftPath.lineTo(transparentRectangleLeft + 100,transparentRectangleBottom + handlerMarginFromCameraCutout)

        bottomLeftPath.moveTo(transparentRectangleLeft - handlerMarginFromCameraCutout,transparentRectangleBottom - 21)
        bottomLeftPath.lineTo(transparentRectangleLeft - handlerMarginFromCameraCutout,transparentRectangleBottom - 100)

        canvasBitmap.drawPath(bottomLeftPath,handlerPaint)
        //canvasBitmap.drawRect(ovalTopBottom,handlerPaint)

        //------------ Paint Bottom Right Conner handler -------------------//

        val ovalBottomRight = RectF(
            transparentRectangleRight - arcPointDiagonalDistance,
            transparentRectangleBottom - arcPointDiagonalDistance,
            transparentRectangleRight + handlerMarginFromCameraCutout,
            transparentRectangleBottom + handlerMarginFromCameraCutout
        )

        val bottomRightPath = Path()
        bottomRightPath.arcTo(ovalBottomRight, 90f, -90f, true)

        bottomRightPath.moveTo(transparentRectangleRight - 21,transparentRectangleBottom + handlerMarginFromCameraCutout)
        bottomRightPath.lineTo(transparentRectangleRight - 100,transparentRectangleBottom + handlerMarginFromCameraCutout)

        bottomRightPath.moveTo(transparentRectangleRight + handlerMarginFromCameraCutout,transparentRectangleBottom - 21)
        bottomRightPath.lineTo(transparentRectangleRight + handlerMarginFromCameraCutout,transparentRectangleBottom - 100)

        canvasBitmap.drawPath(bottomRightPath,handlerPaint)

        return bitmap

    }
}