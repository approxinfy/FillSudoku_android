package com.cyberinfy.fillsudoku

import android.content.Intent
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.text.trimmedLength
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import kotlin.math.pow


class MainActivity : AppCompatActivity() {

    private lateinit var detectedNumbers: TextView
    private lateinit var imageView: ImageView
    private lateinit var btnSolveIt: Button
    private lateinit var btnPick: Button
    private val pickImage = 100

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        imageView = findViewById(R.id.imageView)
        detectedNumbers = findViewById(R.id.detectedNumbers)
        btnSolveIt = findViewById(R.id.buttonSolveIt)
        btnSolveIt.setOnClickListener {
            val sudokuInputMatrix = mutableListOf<MutableList<Int>>()
            val stringMatrix = detectedNumbers.text
            stringMatrix.split("\n").forEach{ line ->
                val currentRow = mutableListOf<Int>()
                line.split("").forEach { element ->
                    val n = element.toString().toIntOrNull()
                    if (n != null){
                        currentRow.add(n)
                    }
                }
                sudokuInputMatrix.add(currentRow)
            }
            solution(sudokuInputMatrix)
        }
        btnPick = findViewById(R.id.buttonPickImage)
        btnPick.setOnClickListener {
            val gallery = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.INTERNAL_CONTENT_URI)
            startActivityForResult(gallery, pickImage)
        }
        picToText()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK && requestCode == pickImage) {
            val imageUri = data?.data
            imageView.setImageURI(imageUri)
            picToText()
        }
    }

    private fun picToText() {
        val recognizer = TextRecognition.getClient()
        val bitmap = (imageView.drawable as BitmapDrawable).bitmap
        val boxes = mutableListOf<Box>()
        var boxWidth = bitmap.width / 9f
        (0..8).forEach { row ->
            (0..8).forEach { column ->
                val positionX = column * boxWidth + boxWidth / 2
                val positionY = row * boxWidth + boxWidth / 2
                boxes.add(Box(positionX, positionY, 0))
            }
        }
        val image = InputImage.fromBitmap(bitmap, 0)
        recognizer.process(image)
            .addOnSuccessListener { visionText ->
                visionText.textBlocks.forEach { textBlock ->
                    textBlock.lines.forEach { line ->
                        line.boundingBox?.let { boundingBox ->
                            boxWidth = boundingBox.width().toFloat() / line.text.trimmedLength()
                            line.text.trim().forEachIndexed { index, number ->
                                val n = number.toString().toIntOrNull()
                                if (n != null) {
                                    val positionX = boundingBox.left + index * boxWidth + boxWidth / 2f
                                    val positionY = boundingBox.top + (boundingBox.bottom - boundingBox.top) / 2f
                                    boxes.indices.minByOrNull { i ->
                                        (positionX - boxes[i].positionX).pow(2) + (positionY - boxes[i].positionY).pow(2)
                                    }?.let { mostSuitableIndex ->
                                        boxes[mostSuitableIndex] = Box(positionX, positionY, n)
                                    }
                                }
                            }
                        }
                    }
                }
                val sudokuInputMatrix = mutableListOf<MutableList<Int>>()
                var rowList = mutableListOf<Int>()
                (0..80).forEach { i ->
                    if (i==0 || i%9==0) {
                        if (i != 0)
                            sudokuInputMatrix.add(rowList)
                        rowList = mutableListOf()
                    }
                    rowList.add(boxes[i].number)
                }
                sudokuInputMatrix.add(rowList)
                detectedNumbers.text = sudokuInputMatrix.joinToString("\n").replace("[","").replace("]","").replace(", ","")
            }
            .addOnFailureListener {
            }
    }

    private fun solution(sudokuInputMatrix: MutableList<MutableList<Int>>): Boolean {
        var terminate = true
        val possibilities = mutableListOf<List<Any>>()
        (0..8).forEach { row ->
            (0..8).forEach { column ->
                if (sudokuInputMatrix[row][column] == 0) {
                    terminate = false
                    val positionPossibilities = mutableListOf<Int>()
                    (1..9).forEach { num ->
                        if (checkPosition(row, column, sudokuInputMatrix, num))
                            positionPossibilities.add(num)
                    }
                    possibilities.add(listOf(row, column, positionPossibilities))
                }
            }
        }
        if (terminate) {
            detectedNumbers.text = sudokuInputMatrix.joinToString("\n").replace("[","").replace("]","").replace(", ","")
            return true
        }
        var row = possibilities[0][0] as Int
        var column = possibilities[0][1] as Int
        @Suppress("UNCHECKED_CAST")
        var positionPossibilities = possibilities[0][2] as MutableList<Int>
        possibilities.forEach { possibility ->
            @Suppress("UNCHECKED_CAST")
            if ((possibility[2] as MutableList<Int>).size < positionPossibilities.size){
                row = possibility[0] as Int
                column = possibility[1] as Int
                positionPossibilities = possibility[2] as MutableList<Int>
            }
        }
        positionPossibilities.forEach{ num ->
            sudokuInputMatrix[row][column] = num
            if (solution(sudokuInputMatrix)) return true
            sudokuInputMatrix[row][column] = 0
        }
        return false
    }

    private fun checkPosition(row: Int, column: Int, sudokuInputMatrix: MutableList<MutableList<Int>>, num: Int): Boolean{
        if(num in sudokuInputMatrix[row])
            return false
        (0..8).forEach{ i ->
            if (sudokuInputMatrix[i][column] == num)
                return false
        }
        val r = row-row%3
        val c = column-column%3
        (0..2).forEach{ i ->
            (0..2).forEach { j ->
                if (sudokuInputMatrix[r+i][c+j] == num) return false
            }
        }
        return true
    }
}
private data class Box(val positionX: Float, val positionY: Float, val number: Int)