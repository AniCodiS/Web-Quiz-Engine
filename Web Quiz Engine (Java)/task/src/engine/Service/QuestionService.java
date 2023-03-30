package engine.Service;

import engine.Entity.Answer;
import engine.Entity.AnswerIndex;
import engine.Entity.Completion;
import engine.Entity.Question;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import java.util.ArrayList;

public interface QuestionService {

    Page<Question> getQuestions(int pageNo, int pageSize, String sortBy);

    void setQuestions(ArrayList<Question> newQuizQuestions);

    Question getById(int id);

    ArrayList<AnswerIndex> getAnswer(int id);

    ArrayList<Answer> getOptions(int id);

    Question addQuestion(Question newQuestion);
    ResponseEntity<String> addQuestionCompletion(int[] guessedAnswers,
                                                        Question requestedQuestion,
                                                        ArrayList<AnswerIndex> AnswerIndexes,
                                                        Authentication auth);
    Page<Completion> getCompletedQuestions(int pageNo, int pageSize, String sortBy);

    void deleteQuestion(Question question);

    int isQuestionOwner(Authentication auth, int id);
}