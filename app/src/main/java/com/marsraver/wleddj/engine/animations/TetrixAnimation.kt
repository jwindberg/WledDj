package com.marsraver.wleddj.engine.animations

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import com.marsraver.wleddj.engine.Animation
import com.marsraver.wleddj.engine.color.Palette
import com.marsraver.wleddj.engine.color.Palettes
import kotlin.math.abs
import kotlin.random.Random

/**
 * Tetrix Animation - AI Bot.
 * 
 * Logic:
 * 1. Spawn Shape inside a grid of dynamic height (to keep blocks square).
 * 2. AI calculates best position (Rotation + X).
 * 3. Piece animates (slides/rotates) to target X.
 * 4. Piece drops.
 * 5. Stack / Clear Lines.
 */
class TetrixAnimation : Animation {

    private var _palette: Palette = Palettes.get("Rainbow") ?: Palettes.getDefault()
    override var currentPalette: Palette?
        get() = _palette
        set(value) { if (value != null) _palette = value }

    override fun supportsPalette(): Boolean = true

    // Params
    private val NUM_COLS = 10
    private var LOGIC_HEIGHT = 20 // Init default, updated dynamic in draw
    private var paramSpeed: Int = 128
    
    // Rendering
    private val paint = Paint().apply { isAntiAlias = true }
    private val strokePaint = Paint().apply { 
        isAntiAlias = true; style = Paint.Style.STROKE; strokeWidth = 2f; color = Color.BLACK; alpha = 50 
    }
    private val blockRect = RectF()
    private val innerRect = RectF()

    // --- Logic Models ---

    // A block in the stack
    data class Block(var x: Int, var y: Int, val color: Int)
    
    // A Shape definition (list of offsets)
    data class ShapeDef(val rotations: List<List<Pair<Int, Int>>>)
    
    // The active falling piece
    data class ActivePiece(
        val typeIdx: Int,
        var rotIdx: Int,
        var x: Float, // Float for smooth sliding
        var y: Float, // Float for smooth falling
        val color: Int,
        
        // AI Target
        var targetX: Int,
        var targetRot: Int
    )

    private val stack = mutableListOf<Block>()
    private var activePiece: ActivePiece? = null
    
    // Tetromino Transforms
    private val SHAPES = listOf(
        // I
        ShapeDef(listOf(
            listOf(-1 to 0, 0 to 0, 1 to 0, 2 to 0),
            listOf(0 to -1, 0 to 0, 0 to 1, 0 to 2)
        )),
        // J
        ShapeDef(listOf(
            listOf(-1 to 0, 0 to 0, 1 to 0, 1 to 1),
            listOf(0 to 0, 0 to 1, 0 to 2, -1 to 2),
            listOf(-1 to 0, -1 to 1, 0 to 1, 1 to 1),
            listOf(0 to 0, 1 to 0, 0 to 1, 0 to 2)
        )),
        // L
        ShapeDef(listOf(
            listOf(-1 to 0, 0 to 0, 1 to 0, -1 to 1),
            listOf(-1 to 0, 0 to 0, 0 to 1, 0 to 2),
            listOf(1 to 0, 1 to 1, 0 to 1, -1 to 1),
            listOf(0 to 0, 0 to 1, 0 to 2, 1 to 2) 
        )),
        // O
        ShapeDef(listOf(
            listOf(0 to 0, 1 to 0, 0 to 1, 1 to 1) 
        )),
        // S
        ShapeDef(listOf(
            listOf(0 to 0, 1 to 0, -1 to 1, 0 to 1),
            listOf(0 to 0, 0 to 1, 1 to 1, 1 to 2)
        )),
        // T
        ShapeDef(listOf(
            listOf(-1 to 0, 0 to 0, 1 to 0, 0 to 1),
            listOf(0 to 0, 0 to 1, 0 to 2, -1 to 1), 
            listOf(-1 to 1, 0 to 1, 1 to 1, 0 to 0),
            listOf(0 to 0, 0 to 1, 0 to 2, 1 to 1) 
        )),
        // Z
        ShapeDef(listOf(
            listOf(-1 to 0, 0 to 0, 0 to 1, 1 to 1),
            listOf(1 to 0, 1 to 1, 0 to 1, 0 to 2)
        ))
    )

    override fun draw(canvas: Canvas, width: Float, height: Float) {
        if (width == 0f) return
        
        // Dynamic Dimensions for perfectly Square blocks
        val colW = width / NUM_COLS
        val rowH = colW // SQUARES!
        
        // Calculate dynamic height to fill screen
        // Ensure at least 20 rows logic
        val screenRows = (height / rowH).toInt() + 1
        // We update LOGIC_HEIGHT to match screen capacity
        // If it changes significantly, we might want to adapt the stack, but for now we just expand/shrink view.
        // Existing positive Y blocks remain valid unless cut off.
        if (LOGIC_HEIGHT != screenRows) {
            LOGIC_HEIGHT = screenRows
        }
        
        // Logic Tick
        updateLogic()
        
        // Draw
        canvas.drawColor(Color.BLACK)
        
        // Draw Stack
        for (b in stack) {
            drawBlock(canvas, b.x.toFloat(), b.y.toFloat(), colW, rowH, b.color)
        }
        
        // Draw Active Piece
        activePiece?.let { p ->
            val shapeBlocks = getShapeBlocks(p.typeIdx, p.rotIdx)
            for ((dx, dy) in shapeBlocks) {
                drawBlock(canvas, p.x + dx, p.y + dy, colW, rowH, p.color)
            }
        }
    }
    
    // --- Logic ---
    
    private fun updateLogic() {
        // Spawn if needed
        if (activePiece == null) {
            spawnPiece()
        }
        
        val p = activePiece ?: return
        
        // AI Control & Gravity
        val moveSpeed = 0.05f + (paramSpeed/255f) * 0.2f
        
        // 1. Rotate towards target
        if (p.rotIdx != p.targetRot) {
            p.rotIdx = p.targetRot
        }
        
        // 2. Slide towards target X
        val dx = p.targetX - p.x
        if (abs(dx) > 0.1f) {
            // Slide
            val slideSpeed = moveSpeed * 1.5f 
            p.x += dx.coerceIn(-slideSpeed, slideSpeed)
        } else {
            p.x = p.targetX.toFloat() // Snap X
            // 3. Drop faster if aligned
            // p.y += moveSpeed * 1.5f // Optional speedup
        }
        
        // Gravity
        p.y += moveSpeed
        
        // Collision / Landing
        if (checkCollision(p)) {
            // Back up one step
            p.y -= moveSpeed
            
            // Lock logic
            lockPiece(p)
            activePiece = null
            checkLines()
        }
    }
    
    // --- AI / Spawning ---
    
    private fun spawnPiece() {
        val type = Random.nextInt(SHAPES.size)
        val color = _palette.getInterpolatedInt(Random.nextInt(256))
        
        // Determine Best Fit
        val heuristic = findBestMove(type)
        
        activePiece = ActivePiece(
            typeIdx = type,
            rotIdx = 0, // start default rotation
            x = (NUM_COLS / 2).toFloat(),
            y = -4f, // start above
            color = color,
            targetX = heuristic.first,
            targetRot = heuristic.second
        )
    }
    
    private fun findBestMove(typeIdx: Int): Pair<Int, Int> {
        var bestScore = -100000.0
        var bestX = 0
        var bestRot = 0
        
        val def = SHAPES[typeIdx]
        
        // For each rotation
        for (rot in def.rotations.indices) {
            val blocks = def.rotations[rot]
            
            val minX = blocks.minOf { it.first }
            val maxX = blocks.maxOf { it.first }
            
            // Attempt drops at every valid column
            for (col in 0 until NUM_COLS) {
                if (col + minX < 0 || col + maxX >= NUM_COLS) continue
                
                // Simulate Drop
                val landY = simulateDropY(blocks, col)
                
                if (landY < 0) {
                     // Bad move
                } else {
                    val score = evaluateGridState(blocks, col, landY)
                    if (score > bestScore) {
                        bestScore = score
                        bestX = col
                        bestRot = rot
                    }
                }
            }
        }
        return bestX to bestRot
    }
    
    private fun simulateDropY(blocks: List<Pair<Int, Int>>, x: Int): Int {
        var y = -4 
        while (true) {
            // check if y+1 collides
            if (checkCollision(blocks, x, y + 1)) {
                return y
            }
            y++
            if (y > LOGIC_HEIGHT) return LOGIC_HEIGHT 
        }
    }
    
    private fun checkCollision(p: ActivePiece): Boolean {
        val ix = (p.x + 0.5f).toInt()
        
        val blocks = getShapeBlocks(p.typeIdx, p.rotIdx)
        for ((dx, dy) in blocks) {
            val bx = ix + dx
            val by = p.y + dy
            
            // Floor
            if (by >= LOGIC_HEIGHT - 1) return true
            
            // Stack
            val stackY = getStackHeightAt(bx) 
            if (by + 1f >= stackY) return true
        }
        return false
    }
    
    private fun checkCollision(blocks: List<Pair<Int, Int>>, x: Int, y: Int): Boolean {
        for ((dx, dy) in blocks) {
            val bx = x + dx
            val by = y + dy
            
            if (by >= LOGIC_HEIGHT) return true 
            if (by < 0) continue 
            
            // Stack collision
            if (stack.any { it.x == bx && it.y == by }) return true
        }
        return false
    }

    private fun getStackHeightAt(col: Int): Int {
        return stack.filter { it.x == col }.minOfOrNull { it.y } ?: LOGIC_HEIGHT
    }
    
    // --- Heuristics ---
    
    private fun evaluateGridState(blocks: List<Pair<Int, Int>>, x: Int, y: Int): Double {
        // Higher y is deeper (better)
        val heightScore = y * 1.0 
        
        var holes = 0
        for ((dx, dy) in blocks) {
            val bx = x + dx
            val by = y + dy
            
            // If (bx, by+1) is empty and valid, and it's not part of our own piece?
            if (by + 1 < LOGIC_HEIGHT) {
                // Is blocked by stack?
                val belowIsStack = stack.any { it.x == bx && it.y == by + 1 }
                // Is occupied by self?
                val belowIsSelf = blocks.any { it.first == dx && it.second == dy + 1 }
                
                if (!belowIsStack && !belowIsSelf) {
                    holes++
                }
            }
        }
        
        return heightScore - (holes * 10.0) 
    }

    // --- Helpers ---
    
    private fun getShapeBlocks(type: Int, rot: Int): List<Pair<Int, Int>> {
        val def = SHAPES[type]
        val r = rot % def.rotations.size
        return def.rotations[r]
    }
    
    private fun lockPiece(p: ActivePiece) {
        val blocks = getShapeBlocks(p.typeIdx, p.rotIdx)
        val ix = p.x.toInt()
        val iy = p.y.round()
        
        for ((dx, dy) in blocks) {
            stack.add(Block(ix + dx, iy + dy, p.color))
        }
    }
    
    private fun Float.round(): Int = (this + 0.5f).toInt()
    
    private fun checkLines() {
        val counts = mutableMapOf<Int, Int>()
        for (b in stack) {
            counts[b.y] = (counts[b.y] ?: 0) + 1
        }
        
        val fullRows = counts.filter { it.value >= NUM_COLS }.keys.sorted()
        
        if (fullRows.isNotEmpty()) {
            for (row in fullRows) {
                stack.removeAll { it.y == row }
                stack.forEach { 
                    if (it.y < row) {
                        it.y += 1 
                    }
                }
            }
        }
        
        // Safety Clean
        if (stack.any { it.y < 0 }) {
            stack.clear()
        }
    }
    
    private fun drawBlock(c: Canvas, gridX: Float, gridY: Float, w: Float, h: Float, color: Int) {
        val x = gridX * w
        val y = gridY * h // Square Logic h == w
        val pad = w * 0.05f
        
        blockRect.set(x + pad, y + pad, x + w - pad, y + h - pad)
        
        // Fill
        paint.color = color
        paint.style = Paint.Style.FILL
        c.drawRoundRect(blockRect, w * 0.05f, w * 0.05f, paint)
        
        // Bezel
        val innerPad = w * 0.15f
        innerRect.set(x + innerPad, y + innerPad, x + w - innerPad, y + h - innerPad)
        paint.color = Color.WHITE
        paint.alpha = 50
        c.drawRoundRect(innerRect, w * 0.02f, w * 0.02f, paint)
        
        // Stroke
        c.drawRoundRect(blockRect, w * 0.05f, w * 0.05f, strokePaint)
    }
}
