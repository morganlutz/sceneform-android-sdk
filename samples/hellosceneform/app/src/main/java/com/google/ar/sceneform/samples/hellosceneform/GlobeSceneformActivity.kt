package com.google.ar.sceneform.samples.hellosceneform

import android.graphics.Color
import android.os.Bundle
import android.view.GestureDetector
import android.view.MotionEvent
import androidx.appcompat.app.AppCompatActivity
import androidx.dynamicanimation.animation.DynamicAnimation
import androidx.dynamicanimation.animation.FlingAnimation
import androidx.dynamicanimation.animation.FloatPropertyCompat
import com.google.ar.sceneform.Node
import com.google.ar.sceneform.math.Quaternion
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.rendering.Light
import com.google.ar.sceneform.rendering.ModelRenderable
import com.google.ar.sceneform.ux.BaseTransformableNode
import com.google.ar.sceneform.ux.SelectionVisualizer
import com.google.ar.sceneform.ux.TransformableNode
import com.google.ar.sceneform.ux.TransformationSystem
import kotlinx.android.synthetic.main.activity_globe_sceneform.*
import java.lang.Math.*

class GlobeSceneformActivity : AppCompatActivity() {

    companion object {
        const val X_AXIS = 0f
        const val Y_AXIS = 0f
        const val Z_AXIS = 0f
    }

    private lateinit var localNode: Node
    private lateinit var rotationProperty: GlobeProperty
    private lateinit var animation: FlingAnimation

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_globe_sceneform)


        localNode = Node().apply {
            localPosition = Vector3(X_AXIS, Y_AXIS, Z_AXIS)
//        localRotation = getRotationQuaternion(STARTING_Y_AXIS_ANGLE.toFloat())
        }
        rotationProperty = object : GlobeProperty("rotation") {
            override fun setValue(globe: Node, value: Float) {
                localNode.localRotation = getRotationQuaternion(value)
            }

            override fun getValue(globe: Node): Float = localNode.localRotation.y
        }
        animation = FlingAnimation(localNode, rotationProperty).apply {
            friction = 0.5F
            minimumVisibleChange = DynamicAnimation.MIN_VISIBLE_CHANGE_ROTATION_DEGREES
        }

        sceneview.setBackgroundColor(Color.WHITE)
        val detector = GestureDetector(this, FlingGestureDetector())
        sceneview.setOnTouchListener { _, event ->
            detector.onTouchEvent(event)
            true
        }

        ModelRenderable.builder()
                .setSource(this, R.raw.andy)
                .build()
                .thenApply { model ->
                    addModelToScene(model)
                }
    }

    override fun onResume() {
        super.onResume()
        sceneview.resume()
    }

    override fun onPause() {
        super.onPause()
        sceneview.pause()
    }


    fun addModelToScene(modelRenderable: ModelRenderable) {
        with(localNode) {
            setParent(sceneview.scene)
            renderable = modelRenderable
            localPosition = Vector3(0.0F, -0.5F, -1F)
        }
        with(sceneview.scene) {
            sunlight?.let {
                it.worldPosition = Vector3.forward()
                it.light = Light.builder(Light.Type.DIRECTIONAL)
                        .setColor(com.google.ar.sceneform.rendering.Color(Color.RED))
                        .setColorTemperature(30F)
                        .build()
            }
            localNode.localScale = Vector3(5f, 5f, 5f)
            addChild(localNode)
        }
    }

    private val quaternion = Quaternion()
    private val rotateVector = Vector3.up()
    private var lastDeltaYAxisAngle = 0F

    private fun getRotationQuaternion(deltaYAxisAngle: Float): Quaternion {
        lastDeltaYAxisAngle = deltaYAxisAngle
        return quaternion.apply {
            val arc = toRadians(deltaYAxisAngle.toDouble())
            val axis = sin(arc / 2.0)
            x = rotateVector.x * axis.toFloat()
            y = rotateVector.y * axis.toFloat()
            z = rotateVector.z * axis.toFloat()
            w = cos(arc / 2.0).toFloat()
            normalize()
        }
    }

    // https://medium.com/@afeozzz/how-we-implemented-3d-cards-in-revolut-fa84203a8f42
    inner class FlingGestureDetector : GestureDetector.SimpleOnGestureListener() {

        val screenDensity = resources.displayMetrics.density
        val CARD_ROTATION_FRICTION = 0.5F
        val SWIPE_THRESHOLD_VELOCITY = 0.2F

        override fun onScroll(e1: MotionEvent, e2: MotionEvent, distanceX: Float, distanceY: Float): Boolean {
            val deltaX = -(distanceX / screenDensity) / CARD_ROTATION_FRICTION
            localNode.localRotation = getRotationQuaternion(lastDeltaYAxisAngle + deltaX)
            return true
        }

        override fun onFling(e1: MotionEvent, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
            if (abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {
                val deltaVelocity = (velocityX / screenDensity) / CARD_ROTATION_FRICTION
                startAnimation(deltaVelocity)
            }
            return true
        }
    }

    private fun startAnimation(velocity: Float) {
        if (!animation.isRunning) {
            animation.setStartVelocity(velocity)
            animation.setStartValue(lastDeltaYAxisAngle)
            animation.start()
        }
    }
}

class NoopSelectionVisualizer : SelectionVisualizer {
    override fun applySelectionVisual(node: BaseTransformableNode?) = Unit
    override fun removeSelectionVisual(node: BaseTransformableNode?) = Unit
}

abstract class GlobeProperty(name: String) : FloatPropertyCompat<Node>(name)