package engine.Service;

import engine.Entity.Answer;
import engine.Entity.AnswerIndex;
import engine.Entity.Completion;
import engine.Entity.Question;

import engine.Repository.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
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

    @Autowired
    private QuestionRepository questionRepo;
    @Autowired
    private AnswerRepository answerRepo;
    @Autowired
    private AnswerIndexRepository answerIndexRepo;
    @Autowired
    private UserRepository userRepo;
    @Autowired
    private CompletionRepository completionRepo;

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
                                                        ArrayList<AnswerIndex> AnswerIndexes,
                                                        Authentication auth) {
        String userEmail = auth.getName();

        Completion newCompletion = new Completion();
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSS");
        String formattedDateTime = now.format(formatter);
        newCompletion.setCompletedAt(formattedDateTime);
        newCompletion.setQuestion(requestedQuestion);
        newCompletion.setUserEmail(userEmail);

        if (guessedAnswers != null) {
            Arrays.sort(guessedAnswers);
        }

        if (requestedQuestion == null) {
            return ResponseEntity.status(404).body("Not Found");
        }
        int[] correctAnswers = new int[AnswerIndexes.size()];
        for (int i = 0; i < AnswerIndexes.size(); i++) {
            correctAnswers[i] = AnswerIndexes.get(i).getIndex();
        }
        Arrays.sort(correctAnswers);

        if (guessedAnswers == null) {
            if (correctAnswers.length == 0) {
                this.completionRepo.save(newCompletion);
                return ResponseEntity.ok().body("{\"success\":true,\"feedback\":\"Congratulations, you're right!\"}");
            } else {
                return ResponseEntity.ok().body("{\"success\":false,\"feedback\":\"Wrong answer! Please try again.\"}");
            }
        }
        if (correctAnswers.length == 0) {
            if (guessedAnswers.length == 0) {
                this.completionRepo.save(newCompletion);
                return ResponseEntity.ok().body("{\"success\":true,\"feedback\":\"Congratulations, you're right!\"}");
            } else {
                return ResponseEntity.ok().body("{\"success\":false,\"feedback\":\"Wrong answer! Please try again.\"}");
            }
        }

        if (correctAnswers.length != guessedAnswers.length) {
            return ResponseEntity.ok().body("{\"success\":false,\"feedback\":\"Wrong answer! Please try again.\"}");
        }
        for (int i = 0; i < correctAnswers.length; i++) {
            if (correctAnswers[i] != guessedAnswers[i]) {
                return ResponseEntity.ok().body("{\"success\":false,\"feedback\":\"Wrong answer! Please try again.\"}");
            }
        }
        this.completionRepo.save(newCompletion);
        return ResponseEntity.ok().body("{\"success\":true,\"feedback\":\"Congratulations, you're right!\"}");
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