package com.chathuranga.shan.imagereaderissue.utilities

import android.graphics.*
import android.util.Log
import android.util.Size
import android.view.SurfaceHolder

class ObjectDetectionOverlaySurfaceHolder: SurfaceHolder.Callback {

    private lateinit var drawingThread: DrawingThread
    private lateinit var objectBound : Rect

    lateinit var objectInBoundCheck: ObjectInBoundCheck

    var safeAreaBound = RectF(0f,0f,0f,0f)
    var previewSize = Size(0,0)
    var analyzedSize = Size(0,0)
    var surfaceTop: Int = 0
    var surfaceWidth: Int = 0
    var surfaceHeight: Int = 0

    companion object{
        private  val TAG = ObjectDetectionOverlaySurfaceHolder::class.java.simpleName
    }

    fun repositionBound(objectBound: Rect){
        this.objectBound = objectBound
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        drawingThread = DrawingThread(holder)
        drawingThread.running = true
        drawingThread.start()
    }

    override fun surfaceChanged(p0: SurfaceHolder, p1: Int, p2: Int, p3: Int) {}

    override fun surfaceDestroyed(p0: SurfaceHolder) {
        var retry = true
        drawingThread.running = false

        while (retry){
            try {
                drawingThread.join()
                retry = false
            } catch (e: InterruptedException) {
            }
        }
    }

    inner class DrawingThread(private val holder: SurfaceHolder?): Thread() {

        var running = false

       /*private fun adjustXCoordinates(valueX: Int): Float{

            return if(previewViewWidth != 0){
                (valueX / analyzedImageWidth.toFloat()) * previewViewWidth.toFloat()
            }else{
                valueX.toFloat()
            }
        }

        private fun adjustYCoordinates(valueY: Int): Float{

            return if(previewViewHeight != 0){
                (valueY / analyzedImageHeight.toFloat()) * previewViewHeight.toFloat()
            }else{
                valueY.toFloat()
            }
        }*/

        private fun drawSafeArea(canvas: Canvas){

            val myPaint = Paint()
            myPaint.color = Color.rgb(98, 70, 0)
            myPaint.strokeWidth = 5f
            myPaint.style = Paint.Style.STROKE

            //val refinedRect = RectF()
            //refinedRect.left = safeAreaBound.left//adjustXCoordinates(barcodeBound.left)
            //refinedRect.right = safeAreaBound.right //adjustXCoordinates(barcodeBound.right)
            //refinedRect.top = safeAreaBound.top //adjustYCoordinates(barcodeBound.top)
            //refinedRect.bottom = 200f //adjustYCoordinates(barcodeBound.bottom)

            canvas.drawRect(safeAreaBound,myPaint)
        }


        private fun drawAdjustedPreviewSize(canvas: Canvas,scaleFactor: Float){

            val myPaint = Paint()
            myPaint.color = Color.rgb(0, 100, 0)
            myPaint.strokeWidth = 5f
            myPaint.style = Paint.Style.STROKE

            val analyzedImageRect = RectF()
            analyzedImageRect.top = surfaceTop.toFloat()
            analyzedImageRect.left = 0f

            if(surfaceWidth < surfaceHeight){
                analyzedImageRect.bottom = analyzedSize.width * scaleFactor + analyzedImageRect.top
                analyzedImageRect.right = analyzedSize.height * scaleFactor
            }else{
                analyzedImageRect.bottom = analyzedSize.height * scaleFactor + analyzedImageRect.top
                analyzedImageRect.right = analyzedSize.width * scaleFactor
            }

            canvas.drawRect(analyzedImageRect,myPaint)
        }

        private fun drawObjectBoundBox(canvas: Canvas,scaleFactor: Float){

            val objectPaint = Paint()
            objectPaint.color = Color.rgb(0, 0, 100)
            objectPaint.strokeWidth = 5f
            objectPaint.style = Paint.Style.STROKE

            val objectBoundRect = RectF()
            objectBoundRect.top = (objectBound.top * scaleFactor) + surfaceTop.toFloat()
            objectBoundRect.left = (objectBound.left * scaleFactor)
            objectBoundRect.right = (objectBound.right * scaleFactor)
            objectBoundRect.bottom = (objectBound.bottom * scaleFactor) + surfaceTop.toFloat()
            objectInBoundCheck.isObjectInbound(objectBoundRect)
            canvas.drawRect(objectBoundRect,objectPaint)
        }

        override fun run() {

            while(running){

                if(::objectBound.isInitialized){

                    val canvas = holder!!.lockCanvas()

                    if (canvas != null) {

                        synchronized(holder) {

                            canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)

                            /*val myPaint = Paint()
                            myPaint.color = Color.rgb(20, 100, 50)
                            myPaint.strokeWidth = 6f
                            myPaint.style = Paint.Style.STROKE

                            val refinedRect = RectF()
                            refinedRect.left = 100f//adjustXCoordinates(barcodeBound.left)
                            refinedRect.right = 200f //adjustXCoordinates(barcodeBound.right)
                            refinedRect.top = 100f //adjustYCoordinates(barcodeBound.top)
                            refinedRect.bottom = 200f //adjustYCoordinates(barcodeBound.bottom)*/

                            drawSafeArea(canvas)
                            //drawPreviewSize(canvas)
                            val scaleFactor = previewSize.width/analyzedSize.width.toFloat()
                            drawAdjustedPreviewSize(canvas,scaleFactor)
                            drawObjectBoundBox(canvas,scaleFactor)

                        }

                        holder.unlockCanvasAndPost(canvas)

                    }else{
                        Log.e(TAG, "Cannot draw onto the canvas as it's null")
                    }

                    try {
                        sleep(30)
                    } catch (e: InterruptedException) {
                        e.printStackTrace()
                    }

                }
            }
        }
    }
}