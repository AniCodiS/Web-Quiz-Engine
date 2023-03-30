package engine.Service;

import com.google.gson.JsonObject;
import engine.Entity.Answer;
import engine.Entity.AnswerIndex;
import engine.Entity.Completion;
import engine.Entity.Question;

import engine.Repository.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

import org.springframework.data.domain.Pageable;

@Service
public class QuestionServiceImpl implements QuestionService {

    private QuestionRepository questionRepo;
    private CompletionRepository completionRepo;

    @Autowired
    public QuestionServiceImpl(QuestionRepository questionRepo, CompletionRepository completionRepo) {
        this.questionRepo = questionRepo;
        this.completionRepo = completionRepo;
    }

    public Page<Question> getQuestions(int pageNo, int pageSize, String sortBy) {
        Pageable paging = PageRequest.of(pageNo, pageSize, Sort.by(sortBy));
        return questionRepo.findAll(paging);
    }

    public void setQuestions(ArrayList<Question> newQuizQuestions) {
        questionRepo.saveAll(newQuizQuestions);
    }

    public Question getById(int id) {
        if (questionRepo.findById(id).isPresent()) {
            return questionRepo.findById(id).get();
        }
        return null;
    }

    public ArrayList<AnswerIndex> getAnswer(int id) {
        Optional<Question> questions = questionRepo.findById(id);
        if (questions.isPresent()) {
            List<AnswerIndex> questionAnswers = questions.get().getAnswer();
            return new ArrayList<>(questionAnswers);
        }
        return null;
    }

    public ArrayList<Answer> getOptions(int id) {
        Optional<Question> questions = questionRepo.findById(id);
        if (questions.isPresent()) {
            List<Answer> questionAnswers = questions.get().getOptions();
            return new ArrayList<>(questionAnswers);
        }
        return null;
    }

    @Transactional
    public Question addQuestion(Question newQuestion) {
        List<Answer> questionAnswers = newQuestion.getOptions();
        for (Answer a : questionAnswers) {
            a.setQuestion(newQuestion);
        }

        List<AnswerIndex> questionAnswerIndexes = newQuestion.getAnswer();
        if (questionAnswerIndexes != null) {
            for (AnswerIndex a : questionAnswerIndexes) {
                a.setQuestion(newQuestion);
            }
        }

        questionRepo.save(newQuestion);

        return newQuestion;
    }

    public ResponseEntity<String> addQuestionCompletion(int[] guessedAnswers,
                                                        Question requestedQuestion,
                                                        ArrayList<AnswerIndex> answerIndexes,
                                                        Authentication auth) {
        String userEmail = auth.getName();

        // Check if the requested question exists
        if (requestedQuestion == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Not Found");
        }

        // Sort guessed and correct answer indexes
        if (guessedAnswers != null) {
            Arrays.sort(guessedAnswers);
        }
        int[] correctAnswers = answerIndexes.stream().mapToInt(AnswerIndex::getIndex).toArray();
        Arrays.sort(correctAnswers);

        // Check if guessed and correct answer indexes match
        boolean isCorrect = Arrays.equals(guessedAnswers, correctAnswers);
        String feedback = isCorrect ? "Congratulations, you're right!" : "Wrong answer! Please try again.";

        // Create completion instance and add it to the table
        Completion newCompletion = new Completion();
        LocalDateTime now = LocalDateTime.now();
        String formattedDateTime = now.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        newCompletion.setCompletedAt(formattedDateTime);
        newCompletion.setQuestion(requestedQuestion);
        newCompletion.setUserEmail(userEmail);
        if (isCorrect) {
            completionRepo.save(newCompletion);
        }

        // Construct response JSON
        JsonObject responseJson = new JsonObject();
        responseJson.addProperty("success", isCorrect);
        responseJson.addProperty("feedback", feedback);

        return ResponseEntity.ok().body(responseJson.toString());
    }

    public Page<Completion> getCompletedQuestions(int pageNo, int pageSize, String sortBy) {
        Pageable paging = PageRequest.of(pageNo, pageSize, Sort.by(sortBy).descending());
        return completionRepo.findAll(paging);
    }

    public void deleteQuestion(Question question) {
        questionRepo.delete(question);
    }

    public int isQuestionOwner(Authentication auth, int id) {
        Question question = getById(id);
        if (question == null) {
            return 2;
        }
        String questionUserEmail = question.getCreatorEmail();
        String userEmail = auth.getName();
        if (questionUserEmail.equals(userEmail)) {
            return 1;
        } else {
            return 0;
        }
    }
}