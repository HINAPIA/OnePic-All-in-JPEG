package com.example.onepic.EditModule.Fragment

import android.annotation.SuppressLint
import android.content.ContentValues.TAG
import android.graphics.*
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Switch
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.onepic.ExPictureContainer
import com.example.onepic.ImageToolModule
import com.example.onepic.Picture
import com.example.onepic.R
import com.example.onepic.databinding.FragmentWarpingBinding
import org.opencv.android.OpenCVLoader
import org.opencv.android.Utils
import org.opencv.core.CvType
import org.opencv.core.CvType.CV_32FC1
import org.opencv.core.Mat
import org.opencv.core.MatOfPoint2f
import org.opencv.core.Scalar
import org.opencv.imgproc.Imgproc
import org.opencv.imgproc.Imgproc.getAffineTransform
import org.opencv.imgproc.Imgproc.warpAffine

enum class TrianglePoint { right, top, left, bottom }
class WarpingFragment : Fragment() {

    private lateinit var binding: FragmentWarpingBinding

    private lateinit var exPictureContainer: ExPictureContainer
    private lateinit var imageToolModule: ImageToolModule

    private lateinit var mainPicture: Picture
    private lateinit var mainBitmap: Bitmap

    private var triangleBitmap: ArrayList<Bitmap> = arrayListOf()
    private var changeTriangleBitmap: ArrayList<Bitmap> = arrayListOf()

    private var clickCount = 0
    private lateinit var touchPoint: Point
    private lateinit var movePoint: Point

    init {
        val isIntialized = OpenCVLoader.initDebug()
        Log.d(TAG, "isIntialized = $isIntialized")
    }
    @SuppressLint("ClickableViewAccessibility")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // 뷰 바인딩 설정
        binding = FragmentWarpingBinding.inflate(inflater, container, false)

        /** ExPictureContainer 설정 **/
        if (arguments != null)
            exPictureContainer =
                requireArguments().getSerializable("exPictureContainer") as ExPictureContainer // Bundle에서 객체를 받아옴
        else
            exPictureContainer = ExPictureContainer(inflater.context)

        imageToolModule = ImageToolModule()

        // main Picture의 byteArray를 bitmap 제작
        mainPicture = exPictureContainer.getMainPicture()
        mainBitmap = imageToolModule.byteArrayToBitmap(mainPicture.byteArray)

        binding.warpingMainView.setImageBitmap(mainBitmap)

        // save btn 클릭 시
        binding.warpingSaveBtn.setOnClickListener {
            mainPicture.byteArray = imageToolModule.bitmapToByteArray(mainBitmap)
            exPictureContainer.setMainPicture(0, mainPicture)

            val bundle = Bundle()
            bundle.putSerializable("exPictureContainer", exPictureContainer) // 객체를 Bundle에 저장
            findNavController().navigate(R.id.action_warpingFragment_to_editFragment, bundle)
        }

        // close btn 클릭 시
        binding.warpingCloseBtn.setOnClickListener {
            val bundle = Bundle()
            bundle.putSerializable("exPictureContainer", exPictureContainer) // 객체를 Bundle에 저장
            findNavController().navigate(R.id.action_warpingFragment_to_editFragment, bundle)
        }


        // 이미지 뷰 클릭 시
        binding.warpingMainView.setOnTouchListener { view, event ->

            if (event!!.action == MotionEvent.ACTION_DOWN) {
                // click 좌표를 bitmap에 해당하는 좌표로 변환
                val point = ImageToolModule().getBitmapClickPoint(
                    PointF(event.x, event.y),
                    view as ImageView
                )

                if(clickCount == 0) {
                    touchPoint = point
                    println("------- click point:$touchPoint")

                    val triangleDrawBitmap = drawTriangle(mainBitmap, touchPoint)

                    triangleBitmap = triangleCropBitmap(mainBitmap, touchPoint)

                    binding.warpingMainView.setImageBitmap(triangleDrawBitmap)

                    clickCount++
                }
                else {
                    movePoint = point
                    changeTriangleBitmap = triangleCropBitmap(mainBitmap, touchPoint)

                    clickCount--

                    val trianglePoint = listOf(TrianglePoint.top, TrianglePoint.left, TrianglePoint.right, TrianglePoint.bottom)

                    var newMainBitmap = triangleBitmap[0]
                    for (i in 0 until triangleBitmap.size) {
                        triangleBitmap[i] =
                            processImage(triangleBitmap[i], Point(movePoint.x - touchPoint.x, movePoint.y - touchPoint.y),trianglePoint[i])
                        newMainBitmap =
                            imageToolModule.overlayBitmap(newMainBitmap, triangleBitmap[i], 0, 0)
                    }

                    binding.warpingMainView.setImageBitmap(newMainBitmap)
                }
            }

            return@setOnTouchListener true
        }


        return binding.root
    }

    private fun drawTriangle(bitmap: Bitmap, touchPoint: Point) : Bitmap {

        val pointX = touchPoint.x.toFloat()
        val pointY = touchPoint.y.toFloat()

        val width = bitmap.width.toFloat()
        val height = bitmap.height.toFloat()

        val outputBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(outputBitmap)
        val pen = Paint()
        pen.textAlign = Paint.Align.LEFT

        // draw bounding box
        pen.color = Color.parseColor("#B8C5BB")
        //pen.color = customColor
        pen.strokeWidth = imageToolModule.floatToDp(3F).toFloat()
        pen.style = Paint.Style.STROKE

        canvas.drawPoint(pointX, pointY, pen)

        canvas.drawLine(0F,0F, pointX, pointY, pen)
        canvas.drawLine(width,0F, pointX, pointY, pen)
        canvas.drawLine(0F, height, pointX, pointY, pen)
        canvas.drawLine(width, height, pointX, pointY, pen)

        return outputBitmap
    }

    private fun triangleCropBitmap(bitmap: Bitmap,touchPoint: Point): ArrayList<Bitmap> {

        val triangleBitmap: ArrayList<Bitmap> = arrayListOf()

        val pointX = touchPoint.x.toFloat()
        val pointY = touchPoint.y.toFloat()

        val width = bitmap.width.toFloat()
        val height = bitmap.height.toFloat()

        // path를 얻고자 하는 모양을 만들어준다.
        var path = Path()
        path.moveTo(0F, 0F)
        path.lineTo(pointX, pointY)
        path.lineTo(width, 0F)
        path.close()

        triangleBitmap.add(cropPathBitmap(bitmap, path))

        // path를 얻고자 하는 모양을 만들어준다.
        path = Path()
        path.moveTo(0F, 0F)
        path.lineTo(pointX, pointY)
        path.lineTo(0F, height)
        path.close()

        triangleBitmap.add(cropPathBitmap(bitmap, path))

        // path를 얻고자 하는 모양을 만들어준다.
        path = Path()
        path.moveTo(width, height)
        path.lineTo(pointX, pointY)
        path.lineTo(width, 0F)
        path.close()

        triangleBitmap.add(cropPathBitmap(bitmap, path))

        // path를 얻고자 하는 모양을 만들어준다.
        path = Path()
        path.moveTo(width, height)
        path.lineTo(pointX, pointY)
        path.lineTo(0F, height)
        path.close()

        triangleBitmap.add(cropPathBitmap(bitmap, path))

        return triangleBitmap
    }

    private fun cropPathBitmap(bitmap: Bitmap, path: Path): Bitmap {

        val resultImg = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        val maskImg = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        val mCanvas = Canvas(resultImg)
        val maskCanvas = Canvas(maskImg)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.isAntiAlias = true
        paint.style = Paint.Style.FILL

        // mask 모양이 될 path를 그려주고
        maskCanvas.drawPath(path, paint)

        // 내부의 영역만 가져오도록 설정한다.
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)

        // canvas에 현재 이미지를 그려준 후, 내부의 영역만 가져올 마스킹을 통해 원하는 부분의 이미지를 얻는다.
        mCanvas.drawBitmap(bitmap, 0F, 0F, null)
        mCanvas.drawBitmap(maskImg, 0F, 0F, paint)

        return resultImg
    }

    private fun processImage(bitmap: Bitmap, point:Point, trianglePoint: TrianglePoint): Bitmap {

        // Bitmap을 Mat 객체로 변환
        val matAddr = Mat()
        Utils.bitmapToMat(bitmap, matAddr)

        // 이미지 포인터 가져오기
        // 이미지 어파인 변환 수행
        val dst = Mat()
        var warpMat = Mat(2, 3, CV_32FC1)

        val srcTri = MatOfPoint2f()
        val dstTri = MatOfPoint2f()

        // 기존에 위치했던 점들의 값
        val pointList: ArrayList<org.opencv.core.Point> = arrayListOf()
        val movePointList: ArrayList<org.opencv.core.Point> = arrayListOf()
        if (trianglePoint == TrianglePoint.left || trianglePoint == TrianglePoint.top) {
            pointList.add(org.opencv.core.Point(0.0, 0.0))
            movePointList.add(org.opencv.core.Point(0.0, 0.0))
        }
        else {
            pointList.add(org.opencv.core.Point((matAddr.cols() - 1).toDouble(),  (matAddr.rows() - 1).toDouble()))
            movePointList.add(org.opencv.core.Point((matAddr.cols() - 1).toDouble(),  (matAddr.rows() - 1).toDouble()))
        }
        pointList.add(org.opencv.core.Point((matAddr.cols() - 1).toDouble(), 0.0))
        pointList.add(org.opencv.core.Point(0.0, (matAddr.rows() - 1).toDouble()))

        srcTri.fromList(pointList)


        if (trianglePoint == TrianglePoint.left || trianglePoint == TrianglePoint.bottom) {
            movePointList.add(
                org.opencv.core.Point(
                    (matAddr.cols() - 1).toDouble() + point.x.toDouble(),
                    point.y.toDouble()
                )
            )
            movePointList.add(org.opencv.core.Point(0.0, (matAddr.rows() - 1).toDouble()))
        } else{
            movePointList.add(org.opencv.core.Point((matAddr.cols() - 1).toDouble(), 0.0))
            movePointList.add(
                org.opencv.core.Point(
                    point.x.toDouble(),
                    (matAddr.rows() - 1).toDouble() + point.y.toDouble()
                )
            )
        }
        dstTri.fromList(movePointList)

        warpMat = getAffineTransform(srcTri, dstTri)
        warpAffine(matAddr, dst, warpMat, matAddr.size())

        var newBitmap: Bitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        Utils.matToBitmap(dst, newBitmap)

        return newBitmap
    }


    private fun transformBitmap(bitmap: Bitmap, point: Point): Bitmap {
        // 이동할 거리
        val dx = point.x.toFloat()
        val dy = point.y.toFloat()

        // 축소할 비율
        val scale = 1 - (point.y.toFloat() / bitmap.height)

        // 변환 행렬 생성
        val matrix = Matrix().apply {
            postTranslate(dx, dy)
            postScale(scale, scale, point.x.toFloat(), point.y.toFloat())
        }

        // 변환된 bitmap 생성
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }
}
