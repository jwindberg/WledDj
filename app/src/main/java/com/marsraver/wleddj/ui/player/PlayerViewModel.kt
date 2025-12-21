package com.marsraver.wleddj.ui.player

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.marsraver.wleddj.data.repository.InstallationRepository
import com.marsraver.wleddj.engine.RenderEngine
import com.marsraver.wleddj.engine.animations.BouncingBallAnimation
import com.marsraver.wleddj.engine.animations.RandomRectsAnimation
import com.marsraver.wleddj.engine.animations.StationaryBallAnimation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PlayerViewModel(
    private val installationId: String,
    private val repository: InstallationRepository
) : ViewModel() {

    private val _engine = MutableStateFlow<RenderEngine?>(null)
    val engine = _engine.asStateFlow()
    
    private val _installation = MutableStateFlow<com.marsraver.wleddj.data.model.Installation?>(null)
    val installation = _installation.asStateFlow()

    init {
        loadInstallation()
    }

    private var _originalInstallation: com.marsraver.wleddj.data.model.Installation? = null

    private fun loadInstallation() {
        viewModelScope.launch {
            val loadedInst = repository.getInstallation(installationId)
            if (loadedInst != null) {
                _originalInstallation = loadedInst
                // Initial load with existing bounds (square/default).
                android.util.Log.d("WledDj", "DEBUG: Loaded ${loadedInst.animations.size} animations")
                // We will resize correctly once UI reports screen size.
                // But initially, ensure we don't clip obviously.
                updateInstallationBounds(loadedInst.width, loadedInst.height)
            }
        }
    }
    
    // Called by UI when screen size is known
    // Called by UI when screen size is known
    fun onViewportSizeChanged(viewW: Float, viewH: Float) {
        val orig = _originalInstallation ?: return
        
        // DISABLE AUTOMATIC SHIFTING/RESIZING
        // We want WYSIWYG from the Editor.
        // If the user placed devices at specific coordinates, we keep them there.
        // The Camera handles the view.
        
        if (_installation.value != orig) {
             // Rebuild logic
        }
        
         _engine.value?.stop()
         _installation.value = orig
         val newEngine = RenderEngine(orig)
         
         // Restore Animations
         orig.animations.forEach { saved ->
             val anim = createAnimation(saved.type)
             val region = com.marsraver.wleddj.data.model.AnimationRegion(
                 id = saved.id,
                 rect = android.graphics.RectF(saved.rectLeft, saved.rectTop, saved.rectRight, saved.rectBottom),
                 rotation = saved.rotation,
                 animation = anim
             )
             newEngine.addRegion(region)
         }
         
         _engine.value = newEngine
         newEngine.start()
         refreshRegions()
    }
    
    // Helper used by loadInstallation if viewport not ready
    private fun updateInstallationBounds(w: Float, h: Float) {
         val orig = _originalInstallation ?: return
         _engine.value?.stop()
         _installation.value = orig
         val newEngine = RenderEngine(orig)
         
         // Restore Animations
         orig.animations.forEach { saved ->
             val anim = createAnimation(saved.type)
             val region = com.marsraver.wleddj.data.model.AnimationRegion(
                 id = saved.id,
                 rect = android.graphics.RectF(saved.rectLeft, saved.rectTop, saved.rectRight, saved.rectBottom),
                 rotation = saved.rotation,
                 animation = anim
             )
             newEngine.addRegion(region)
         }
         
         _engine.value = newEngine
         newEngine.start()
         refreshRegions()
    }

    // Regions State
    private val _regions = MutableStateFlow<List<com.marsraver.wleddj.data.model.AnimationRegion>>(emptyList())
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
        
        val region = com.marsraver.wleddj.data.model.AnimationRegion(
            rect = regionSize.let { s -> android.graphics.RectF(100f, 100f, 100f+s, 100f+s) }, // explicit
            animation = BouncingBallAnimation(50f, 50f, 30f)
        )
        _engine.value?.addRegion(region)
        refreshRegions()
    }
    
    fun addRectAnimation() {
        val region = com.marsraver.wleddj.data.model.AnimationRegion(
            rect = android.graphics.RectF(200f, 400f, 600f, 800f),
            animation = RandomRectsAnimation()
        )
         _engine.value?.addRegion(region)
         refreshRegions()
    }
    
    fun updateRegion(id: String, newRect: android.graphics.RectF, rotation: Float) {
        _engine.value?.updateRegion(id, newRect, rotation)
        refreshRegions()
        // Removed saveAnimations() to prevent flooding. 
        // Logic moved to onInteractionEnd in UI.
    }
    
    fun removeRegion(id: String) {
        _engine.value?.removeRegion(id)
        refreshRegions()
        saveAnimations()
    }

    fun clearAnimations() {
        _engine.value?.clearAnimations()
        refreshRegions()
        saveAnimations()
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
    
    // Selection State
    private val _selectedRegionId = MutableStateFlow<String?>(null)
    val selectedRegionId: StateFlow<String?> = _selectedRegionId.asStateFlow()

    fun selectRegion(id: String?) {
        _selectedRegionId.value = id
    }

    // Consolidated Delete Action
    fun deleteSelection() {
        val selected = _selectedRegionId.value
        if (selected != null) {
            removeRegion(selected)
            _selectedRegionId.value = null
        }
    }

    fun createAnimation(type: String, dropX: Float = 0f, dropY: Float = 0f): com.marsraver.wleddj.engine.Animation {
         return when(type) {
            "Ball" -> com.marsraver.wleddj.engine.animations.BouncingBallAnimation(dropX, dropY, 30f)
            "Static" -> com.marsraver.wleddj.engine.animations.StationaryBallAnimation()
            "Rects" -> com.marsraver.wleddj.engine.animations.RandomRectsAnimation()
            "Fireworks" -> com.marsraver.wleddj.engine.animations.FireworksAnimation()
            "Aurora Borealis" -> com.marsraver.wleddj.engine.animations.AuroraBorealisAnimation()
            else -> com.marsraver.wleddj.engine.animations.BouncingBallAnimation(dropX, dropY, 30f)
        }
    }
    
    // Made Public for UI to call on Drag End
    fun saveAnimations() {
        val currentEngine = _engine.value ?: return
        val currentInst = _installation.value ?: return
        
        val regions = currentEngine.getRegions()
        val savedList = regions.map { region ->
            val type = when(region.animation) {
                is com.marsraver.wleddj.engine.animations.BouncingBallAnimation -> "Ball"
                is com.marsraver.wleddj.engine.animations.StationaryBallAnimation -> "Static"
                is com.marsraver.wleddj.engine.animations.RandomRectsAnimation -> "Rects"
                is com.marsraver.wleddj.engine.animations.FireworksAnimation -> "Fireworks"
                is com.marsraver.wleddj.engine.animations.AuroraBorealisAnimation -> "Aurora Borealis"
                else -> "Ball"
            }
            
            com.marsraver.wleddj.data.model.SavedAnimation(
                id = region.id,
                type = type,
                rectLeft = region.rect.left,
                rectTop = region.rect.top,
                rectRight = region.rect.right,
                rectBottom = region.rect.bottom,
                rotation = region.rotation
            )
        }
        
        val newInst = currentInst.copy(animations = savedList)
        
        // UNCONDITIONAL UPDATE of Original to prevent Zombie state
        _originalInstallation = newInst
        _installation.value = newInst
        
        // Only write to disk if changed (optimization)
        if (currentInst.animations != savedList) {
            android.util.Log.d("WledDj", "DEBUG: Saving ${savedList.size} animations")
            viewModelScope.launch {
                repository.updateInstallation(newInst)
            }
        }
    }

    fun onToolDropped(type: String, dropX: Float, dropY: Float, installW: Float, installH: Float) {
        val size = 300f // Default Size
        
        val animation = createAnimation(type, dropX, dropY)
        
        // Center the rect on the drop point
        val region = com.marsraver.wleddj.data.model.AnimationRegion(
            rect = android.graphics.RectF(dropX - size/2, dropY - size/2, dropX + size/2, dropY + size/2),
            animation = animation
        )
        _engine.value?.addRegion(region)
        refreshRegions()
        
        // Auto-select
        val regions = _regions.value
        _selectedRegionId.value = regions.lastOrNull()?.id
        
        saveAnimations()
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
