package udit.programmer.co.imageeditor

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Matrix
import android.graphics.PointF
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.activity_main.*
import kotlin.math.sqrt

private var TAG = "CEASED_METEOR"

class MainActivity : AppCompatActivity() {
    private var imagePickCode = 1234
    private var permissionCode = 4321

    var matrix = Matrix()
    var savedMatrix = Matrix()

    val NONE = 0
    val DRAG = 1
    val ZOOM = 2
    var mode = NONE

    var start = PointF()
    var mid = PointF()
    var oldDist = 1f
    var lastEvent: FloatArray? = null

    var newRot = 0F
    var d = 0F

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                permissionCode
            )
        }
        add_image_btn.setOnClickListener {
            startActivityForResult(Intent(Intent.ACTION_PICK), imagePickCode)
        }


    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode == 2222) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                add_image_btn.setOnClickListener {
                    startActivityForResult(Intent(Intent.ACTION_PICK), imagePickCode)
                }
            } else {
                Toast.makeText(this, "Grant Premissions", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == imagePickCode && resultCode == Activity.RESULT_OK) {
            Toast.makeText(this, "Image Selected :)", Toast.LENGTH_LONG)
            Picasso.get().load(data!!.data).into(imageViewMain)
            editingWork()
        }
    }

    private fun dumpEvent(event: MotionEvent) {
        val names = arrayOf(
            "DOWN", "UP", "MOVE", "CANCEL", "OUTSIDE",
            "POINTER_DOWN", "POINTER_UP", "7?", "8?", "9?"
        )
        val sb = StringBuilder()
        val action = event.action
        val actionCode = action and MotionEvent.ACTION_MASK
        sb.append("event ACTION_").append(names[actionCode])
        if (actionCode == MotionEvent.ACTION_POINTER_DOWN
            || actionCode == MotionEvent.ACTION_POINTER_UP
        ) {
            sb.append("(pid ").append(
                action shr MotionEvent.ACTION_POINTER_ID_SHIFT
            )
            sb.append(")")
        }
        sb.append("[")
        for (i in 0 until event.pointerCount) {
            sb.append("#").append(i)
            sb.append("(pid ").append(event.getPointerId(i))
            sb.append(")=").append(event.getX(i).toInt())
            sb.append(",").append(event.getY(i).toInt())
            if (i + 1 < event.pointerCount) sb.append(";")
        }
        sb.append("]")
        Log.d(TAG, sb.toString())
    }

    private fun rotation(event: MotionEvent): Float {
        val delta_x = (event.getX(0) - event.getX(1)).toDouble()
        val delta_y = (event.getY(0) - event.getY(1)).toDouble()
        val radians = Math.atan2(delta_y, delta_x)
        return Math.toDegrees(radians).toFloat()
    }

    private fun spacing(event: MotionEvent): Float {
        val x = event.getX(0) - event.getX(1)
        val y = event.getY(0) - event.getY(1)
        return sqrt(x * x + y * y)
    }

    private fun midPoint(point: PointF, event: MotionEvent) {
        val x = event.getX(0) + event.getX(1)
        val y = event.getY(0) + event.getY(1)
        point[x / 2] = y / 2
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun editingWork() {
        imageViewMain.setOnTouchListener { view, motionEvent ->
            val view = view as ImageView
            view.scaleType = ImageView.ScaleType.MATRIX
            val scale: Float

            dumpEvent(motionEvent)

            when (motionEvent.action and MotionEvent.ACTION_MASK) {
                MotionEvent.ACTION_DOWN -> {
                    savedMatrix.set(matrix)
                    start.set(motionEvent.x, motionEvent.y)
                    Log.d(TAG, "mode=DRAG")
                    mode = DRAG
                }
                MotionEvent.ACTION_POINTER_DOWN -> {
                    oldDist = spacing(motionEvent)
                    if (oldDist > 10f) {
                        savedMatrix.set(matrix)
                        midPoint(mid, motionEvent)
                        mode = ZOOM
                    }
                    lastEvent = FloatArray(4)
                    lastEvent!![0] = motionEvent.getX(0)
                    lastEvent!![1] = motionEvent.getX(1)
                    lastEvent!![2] = motionEvent.getY(0)
                    lastEvent!![3] = motionEvent.getY(1)
                    d = rotation(motionEvent)
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> {
                    mode = NONE
                    Log.d(TAG, "mode=NONE")
                }
                MotionEvent.ACTION_MOVE -> if (mode == DRAG) {
                    matrix.set(savedMatrix)
                    matrix.postTranslate(
                        motionEvent.x - start.x, motionEvent.y
                                - start.y
                    )
                } else if (mode == ZOOM && motionEvent.getPointerCount() == 2) {
                    val newDist = spacing(motionEvent)
                    matrix.set(savedMatrix)
                    if (newDist > 10f) {
                        scale = newDist / oldDist
                        matrix.postScale(scale, scale, mid.x, mid.y)
                    }
                    if (lastEvent != null) {
                        newRot = rotation(motionEvent)
                        val r = newRot - d
                        matrix.postRotate(
                            r, (view.measuredWidth / 2).toFloat(),
                            (view.measuredHeight / 2).toFloat()
                        )
                    }
                }
            }
            view.imageMatrix = matrix
            true
        }
    }

}