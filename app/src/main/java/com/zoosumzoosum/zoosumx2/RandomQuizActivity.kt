package com.zoosumzoosum.zoosumx2

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.android.synthetic.main.activity_random_quiz.*
import java.util.*
import kotlin.random.Random

class RandomQuizActivity : AppCompatActivity() {

    var fbAuth: FirebaseAuth? = null
    var fbFirestore: FirebaseFirestore? = null

    // 백 버튼 금지
    override fun onBackPressed() {
    }

    @SuppressLint("CutPasteId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_random_quiz)

        fbAuth = FirebaseAuth.getInstance()
        fbFirestore = FirebaseFirestore.getInstance()

        val backButton = findViewById<ImageButton>(R.id.random_quiz_back)
        backButton.setOnClickListener {
            finish()
        }

        var correctAnswer = findViewById<Button>(R.id.button_correct_answer_quiz)
        var wrongAnswer = findViewById<Button>(R.id.button_wrong_answer_quiz)
        val nextButton = findViewById<Button>(R.id.random_quiz_next)
        lateinit var correctSubComment: String
        lateinit var wrongSubComment: String

        //정답 문항의 자리 랜덤으로 교환
        val randomAnswerNum = Random.nextInt(2)
        if (randomAnswerNum == 0) {
            correctAnswer = findViewById(R.id.button_wrong_answer_quiz)
            wrongAnswer = findViewById(R.id.button_correct_answer_quiz)
        }

        //DB에서 문제 및 문항 가져오기
        //해당 달의 숫자가 이름인 문서에 저장한 문제를 가져옴
        //문제가 길어질 경우 줄 바꿈은 DB 저장 시 bb를 사용함
        val today: Calendar = Calendar.getInstance()
        val weekly: String = ((today.get(Calendar.MONTH))+1).toString()
        fbFirestore?.collection("senseQuiz")?.document(weekly)
            ?.addSnapshotListener { documentSnapshot, firebaseFirestoreException ->
                if (documentSnapshot == null) return@addSnapshotListener
                random_quiz_main_ment?.text =
                    documentSnapshot.data?.get("title").toString().replace("bb", "\n")
                correctAnswer?.text = documentSnapshot.data?.get("option1").toString()
                wrongAnswer?.text = documentSnapshot.data?.get("option2").toString()
                correctSubComment = documentSnapshot.data?.get("correctSubMent").toString()
                wrongSubComment = documentSnapshot.data?.get("wrongSubMent").toString()
            }


        correctAnswer.setOnClickListener {
            if (wrongAnswer.isSelected) {
                wrongAnswer.isSelected = false
                wrongAnswer.setTextColor(ContextCompat.getColor(this, R.color.colorText))
            }
            correctAnswer.isSelected = true
            correctAnswer.setTextColor(ContextCompat.getColor(this, R.color.colorWhite))
        }

        wrongAnswer.setOnClickListener {
            if (correctAnswer.isSelected) {
                correctAnswer.isSelected = false
                correctAnswer.setTextColor(ContextCompat.getColor(this, R.color.colorText))
            }
            wrongAnswer.isSelected = true
            wrongAnswer.setTextColor(ContextCompat.getColor(this, R.color.colorWhite))
        }


        nextButton.setOnClickListener {
            random_quiz_next.text = "리워드 확인"

            //firestore의 mission 수행 여부 true로 변경
            val missionFlag = hashMapOf(
                "missionSenseQuiz" to "true"
            )
            fbFirestore?.collection("users")?.document(fbAuth?.uid.toString())
                ?.collection("mission")?.document(fbAuth?.uid.toString())
                ?.set(missionFlag, SetOptions.merge())

            if (correctAnswer.isSelected || wrongAnswer.isSelected) {
                val answerChecked: Boolean

                if (correctAnswer.isSelected) {
                    answerChecked = true
                    random_quiz_main_ment?.text = "정답이에요!"
                    random_quiz_image.setImageResource(R.drawable.you_right)

                    random_quiz_sub_ment?.text = correctSubComment
                    correctAnswer.setBackgroundResource(R.drawable.random_correct_correct_color)
                    correctAnswer.setTextColor(Color.WHITE)

                    wrongAnswer.setBackgroundResource(R.drawable.random_correct_wrong_color)
                    wrongAnswer.setTextColor(ContextCompat.getColor(this, R.color.colorSoftGray))

                } else {
                    answerChecked = false
                    random_quiz_main_ment?.text = "정답이...아니에요"
                    random_quiz_image.setImageResource(R.drawable.you_wrong)
                    random_quiz_sub_ment?.text = wrongSubComment
                    wrongAnswer.setBackgroundResource(R.drawable.random_wrong_correct_color)
                    wrongAnswer.setTextColor(Color.WHITE)

                    correctAnswer.setBackgroundResource(R.drawable.random_wrong_wrong_color)
                    correctAnswer.setTextColor(ContextCompat.getColor(this, R.color.colorSoftGray))
                }

                nextButton.setOnClickListener {

                    val intent = Intent(this, GetRewardActivity::class.java)

                    if (answerChecked) {
                        fbFirestore?.collection("users")?.document(fbAuth?.uid.toString())
                            ?.update("rewardPoint", FieldValue.increment(2))
                        intent.putExtra("reward", 2)
                    } else {
                        // 리워드 제공
                        fbFirestore?.collection("users")?.document(fbAuth?.uid.toString())
                            ?.update("rewardPoint", FieldValue.increment(1))
                        intent.putExtra("reward", 1)
                    }
                    startActivity(intent)
                }
            }
            // 아무것도 선택하지 않은 경우 -> 정답 확인 버튼 비활성화
            else {
                Toast.makeText(applicationContext, "정답을 선택해주세요", Toast.LENGTH_LONG).show()
            }

        }
    }
}