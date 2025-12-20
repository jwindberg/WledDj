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

    private var _originalInstallation: com.example.wleddj.data.model.Installation? = null

    private fun loadInstallation() {
        viewModelScope.launch {
            val loadedInst = repository.getInstallation(installationId)
            if (loadedInst != null) {
                _originalInstallation = loadedInst
                // Initial load with existing bounds (square/default).
                // We will resize correctly once UI reports screen size.
                // But initially, ensure we don't clip obviously.
                updateInstallationBounds(loadedInst.width, loadedInst.height)
            }
        }
    }
    
    // Called by UI when screen size is known
    fun onViewportSizeChanged(viewW: Float, viewH: Float) {
        val orig = _originalInstallation ?: return
        
        // Target Aspect Ratio
        val screenRatio = if (viewW > 0) viewH / viewW else 2.0f
        
        // 1. Determine Bounds of Content
        var minX = 0f
        var minY = 0f
        var maxX = orig.width
        var maxY = orig.height
        
        orig.devices.forEach { d ->
            if (d.x < minX) minX = d.x
            if (d.y < minY) minY = d.y
            if (d.x + d.width > maxX) maxX = d.x + d.width
            if (d.y + d.height > maxY) maxY = d.y + d.height
        }
        
        // 2. Normalize Coordinates (Shift Negative ONLY)
        // We only shift if content is Out of Bounds (Negative).
        // If content is at +200, we PRESERVE that margin (User Intent).
        // We also ensure Width is at least Original Width (1000) to prevent usage-based zooming.
        
        val normalizeX = if (minX < 0f) -minX else 0f
        val normalizeY = if (minY < 0f) -minY else 0f
        
        // Calculate Content Dimensions
        // We need enough width to hold the Rightmost Point (maxX + shift).
        // AND we default to at least orig.width (1000) to keep scale consistent (1 Unit = 1/1000 Screen).
        
        val neededW = maxX + normalizeX
        val contentW = maxCode(neededW, orig.width)
        val contentH = maxY + normalizeY
        
        // 3. Aspect Ratio Padding
        // We want finalW to cover content.
        // We want finalH to cover content AND satisfy aspect ratio.
        
        var finalW = contentW
        var finalH = contentW * screenRatio
        
        // If aspect fit is too short for content, expand height
        if (contentH > finalH) {
             finalH = contentH
             // If we expanded height, do we need to expand width to keep ratio?
             // Usually no, we accept scrolling or "Zoom Out" (Letterboxing on sides).
             // But let's stick to width-based scale.
        }
        
        // 4. Centering Logic (Vertical) - REMOVED
        // User prefers WYSIWYG from top-left. Centering introduces "spacers" that shift 
        // top-aligned devices down. We just anchor to 0,0 (after normalization).
        
        // Total Shift = Normalize Only
        val totalShiftX = normalizeX
        val totalShiftY = normalizeY
        
        // 5. Apply Shift to Devices
        val shiftedDevices = orig.devices.map { d ->
            d.copy(x = d.x + totalShiftX, y = d.y + totalShiftY)
        }
        
        val runtimeInst = orig.copy(
             width = finalW + 1f, // +1 buffer
             height = finalH + 1f,
             devices = shiftedDevices
        )
        
        // Update State
        if (_installation.value != runtimeInst) {
             _installation.value = runtimeInst
             val newEngine = RenderEngine(runtimeInst)
             _engine.value = newEngine
             newEngine.start()
        }
    }
    
    // Helper used by loadInstallation if viewport not ready
    private fun updateInstallationBounds(w: Float, h: Float) {
         // Fallback default
         // Just load as is
         val orig = _originalInstallation ?: return
         _installation.value = orig
         val newEngine = RenderEngine(orig)
         _engine.value = newEngine
         newEngine.start()
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
    
    fun onToolDropped(type: String, dropX: Float, dropY: Float, installW: Float, installH: Float) {
        val animation = when(type) {
            "Ball" -> com.example.wleddj.engine.animations.BouncingBallAnimation(dropX, dropY, 30f)
            "Static" -> com.example.wleddj.engine.animations.StationaryBallAnimation()
            "Rects" -> com.example.wleddj.engine.animations.RandomRectsAnimation()
            else -> com.example.wleddj.engine.animations.BouncingBallAnimation(dropX, dropY, 30f)
        }
        
        // Create a region that covers the entire installation (which is already expanded to fit devices)
        val region = com.example.wleddj.data.model.AnimationRegion(
            rect = android.graphics.RectF(0f, 0f, installW, installH),
            animation = animation
        )
        _engine.value?.addRegion(region)
        refreshRegions()
    }
    
    private fun minCode(a: Float, b: Float): Float = if (a < b) a else b
    private fun maxCode(a: Float, b: Float): Float = if (a > b) a else b

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
