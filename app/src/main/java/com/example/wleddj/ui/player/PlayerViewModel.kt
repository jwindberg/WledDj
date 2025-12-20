package com.example.wleddj.ui.player

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.wleddj.data.repository.InstallationRepository
import com.example.wleddj.engine.RenderEngine
import com.example.wleddj.engine.animations.BouncingBallAnimation
import com.example.wleddj.engine.animations.RandomRectsAnimation
import com.example.wleddj.engine.animations.StationaryBallAnimation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PlayerViewModel(
    private val installationId: String,
    private val repository: InstallationRepository
) : ViewModel() {

    private val _engine = MutableStateFlow<RenderEngine?>(null)
    val engine = _engine.asStateFlow()
    
    private val _installation = MutableStateFlow<com.example.wleddj.data.model.Installation?>(null)
    val installation = _installation.asStateFlow()

    init {
        loadInstallation()
    }

    private fun loadInstallation() {
        viewModelScope.launch {
            val installation = repository.getInstallation(installationId)
            if (installation != null) {
                _installation.value = installation
                // Initialize Engine
                val newEngine = RenderEngine(installation)
                _engine.value = newEngine
                newEngine.start()
            }
        }
    }

    // Regions State
    private val _regions = MutableStateFlow<List<com.example.wleddj.data.model.AnimationRegion>>(emptyList())
    val regions = _regions.asStateFlow()

    fun refreshRegions() {
         _regions.value = _engine.value?.getRegions() ?: emptyList()
    }

    fun addTestAnimation() {
        // Add a region in the center
        val regionSize = 300f
        val installation = _engine.value?.let { 
             // We don't have direct access to installation dims here easily without refactor, 
             // but we can assume safe defaults or update model to expose it.
             // For now, let's just create a rect.
             android.graphics.RectF(100f, 100f, 100f + regionSize, 100f + regionSize)
        } ?: android.graphics.RectF(0f, 0f, 300f, 300f)
        
        val region = com.example.wleddj.data.model.AnimationRegion(
            rect = regionSize.let { s -> android.graphics.RectF(100f, 100f, 100f+s, 100f+s) }, // explicit
            animation = BouncingBallAnimation(50f, 50f, 30f)
        )
        _engine.value?.addRegion(region)
        refreshRegions()
    }
    
    fun addRectAnimation() {
        val region = com.example.wleddj.data.model.AnimationRegion(
            rect = android.graphics.RectF(200f, 400f, 600f, 800f),
            animation = RandomRectsAnimation()
        )
         _engine.value?.addRegion(region)
         refreshRegions()
    }
    
    fun updateRegion(id: String, newRect: android.graphics.RectF, rotation: Float) {
        _engine.value?.updateRegion(id, newRect, rotation)
        refreshRegions()
    }
    
    fun removeRegion(id: String) {
        _engine.value?.removeRegion(id)
        refreshRegions()
    }

    fun clearAnimations() {
        _engine.value?.clearAnimations()
        refreshRegions()
    }

    // Tool Drag State
    private val _draggedTool = MutableStateFlow<String?>(null)
    val draggedTool = _draggedTool.asStateFlow()
    
    // We need position to render the ghost
    private val _dragPosition = MutableStateFlow(androidx.compose.ui.geometry.Offset.Zero)
    val dragPosition = _dragPosition.asStateFlow()

    fun startToolDrag(type: String, offset: androidx.compose.ui.geometry.Offset) {
        _draggedTool.value = type
        _dragPosition.value = offset
    }
    
    fun updateToolDrag(delta: androidx.compose.ui.geometry.Offset) {
         _dragPosition.value += delta
    }
    
    fun endToolDrag() {
        _draggedTool.value = null
    }
    
    fun onToolDropped(type: String, rect: android.graphics.RectF) {
        val animation = when(type) {
            "Ball" -> com.example.wleddj.engine.animations.BouncingBallAnimation(50f, 50f, 30f)
            "Static" -> com.example.wleddj.engine.animations.StationaryBallAnimation()
            "Rects" -> com.example.wleddj.engine.animations.RandomRectsAnimation()
            else -> com.example.wleddj.engine.animations.BouncingBallAnimation(50f, 50f, 30f)
        }
        val region = com.example.wleddj.data.model.AnimationRegion(
            rect = rect,
            animation = animation
        )
        _engine.value?.addRegion(region)
        refreshRegions()
    }

    // Interaction Mode
    private val _isInteractiveMode = MutableStateFlow(false)
    val isInteractiveMode = _isInteractiveMode.asStateFlow()
    
    fun toggleInteractiveMode() {
        _isInteractiveMode.value = !_isInteractiveMode.value
    }
    
    fun handleCanvasTouch(x: Float, y: Float) {
        if (_isInteractiveMode.value) {
            _engine.value?.handleTouch(x, y)
        }
    }

    override fun onCleared() {
        super.onCleared()
        _engine.value?.stop()
    }

    class Factory(
        private val installationId: String,
        private val repository: InstallationRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return PlayerViewModel(installationId, repository) as T
        }
    }
}
