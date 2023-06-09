package engine.Controller;

import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import engine.Entity.*;
import engine.Service.QuestionService;
import engine.Service.QuestionServiceImpl;
import engine.Service.UserService;
import engine.Service.UserServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;

import java.io.IOException;
import java.util.*;

@RestController
public class QuizController {

    private final QuestionService newQuiz;

    private final UserService users;

    @Autowired
    public QuizController(QuestionServiceImpl newQuiz, UserServiceImpl users) {
        this.newQuiz = newQuiz;
        this.users = users;
    }

    @PostMapping("/api/quizzes")
    public ResponseEntity<String> addQuestion(@RequestBody Question newQuestion) {
        if (!newQuestion.isValid()) {
            return ResponseEntity.status(400).body("Bad Request");
        }

        // Set creators email for this question
        newQuestion.setCreatorEmail((SecurityContextHolder.getContext().getAuthentication().getName()));

        // Add question to the quiz base
        newQuiz.addQuestion(newQuestion);

        return ResponseEntity.ok(newQuestion.toJson());
    }

    @GetMapping("/api/quizzes/{id}")
    public ResponseEntity<String> getQuestion(@PathVariable("id") int id) throws IOException {
        Question requestedQuestion = newQuiz.getById(id);

        // Check if a question with this id exists in the quiz base
        if (requestedQuestion == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok().body(requestedQuestion.toJson());
    }

    @GetMapping("/api/quizzes")
    public ResponseEntity<String> getAllQuestions(
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(defaultValue = "id") String sortBy
    ) {
        Page<Question> pageResult = newQuiz.getQuestions(page, pageSize, sortBy);

        // Creating response json string with HashMap
        Map<String, Object> responseMap = createResponseMap(pageResult, page, pageSize);
        String finalJsonString = responseMap.toString();

        return ResponseEntity.ok(finalJsonString);
    }

    private Map<String, Object> createResponseMap(Page<Question> pageResult, int page, int pageSize) {
        List<Question> allQuestions = pageResult.getContent();

        int totalElements = (int) pageResult.getTotalElements();
        int totalPages = (totalElements + pageSize - 1) / pageSize;
        boolean isFirst = page == 0;
        boolean isLast = page == totalPages - 1;
        JsonArray contentArray = new JsonArray();
        for (Question question : allQuestions) {
            contentArray.add(JsonParser.parseString(question.toJson()));
        }

        Map<String, Object> responseMap = new HashMap<>();
        responseMap.put("totalPages", totalPages);
        responseMap.put("totalElements", totalElements);
        responseMap.put("last", isLast);
        responseMap.put("first", isFirst);
        responseMap.put("sort", new HashMap<>());
        responseMap.put("number", page);
        responseMap.put("numberOfElements", allQuestions.size());
        responseMap.put("size", pageSize);
        responseMap.put("empty", allQuestions.isEmpty());
        responseMap.put("pageable", new HashMap<>());
        responseMap.put("content", contentArray);

        return responseMap;
    }

    @GetMapping("/api/quizzes/completed")
    public ResponseEntity<String> getCompleted(@RequestParam(required = false) Integer page,
                                               @RequestParam(required = false) Integer pageSize,
                                               @RequestParam(defaultValue = "completedAt") String sortBy,
                                               Authentication auth) {
        // Validation for request parameters
        if (page == null) {
            page = 0;
        }
        if (pageSize == null) {
            pageSize = 10;
        }

        Page<Completion> pageResult = this.newQuiz.getCompletedQuestions(0, 50, sortBy);
        List<Completion> allCompletions = pageResult.getContent();

        // Only getting completions for this specific user
        List<Completion> allUserCompletions = new ArrayList<>();
        for (Completion completion : allCompletions) {
            if (auth.getName().equals(completion.getUserEmail())) {
                allUserCompletions.add(completion);
            }
        }

        int totalElements = allUserCompletions.size();
        int expectedPageSize = Math.min(totalElements, 10);
        pageSize = Math.min(pageSize, expectedPageSize);
        int totalPages = (totalElements + pageSize - 1) / pageSize;
        boolean isFirst = page == 0;
        boolean isLast = page == totalPages - 1;

        // Parsing Completion instances to json format
        JsonArray contentArray = new JsonArray();
        int startIndex = page * pageSize;
        int endIndex = Math.min(startIndex + pageSize, totalElements);
        for (int i = startIndex; i < endIndex; i++) {
            Completion completion = allUserCompletions.get(i);
            contentArray.add(JsonParser.parseString(completion.toJson()));
        }

        // Generating HashMap to transform into json format string
        Map<String, Object> responseMap = new HashMap<>();
        responseMap.put("totalPages", totalPages);
        responseMap.put("totalElements", totalElements);
        responseMap.put("last", isLast);
        responseMap.put("first", isFirst);
        responseMap.put("empty", allCompletions.isEmpty());
        responseMap.put("content", contentArray);
        String finalJsonString = responseMap.toString();

        return ResponseEntity.ok(finalJsonString);
    }

    @PostMapping("/api/quizzes/{id}/solve")
    public ResponseEntity<String> solve(@PathVariable("id") int id,
                                        @RequestBody QuizAnswer answer,
                                        Authentication auth) {
        int[] guessedAnswers = answer.getAnswer();
        Question requestedQuestion = newQuiz.getById(id);
        ArrayList<AnswerIndex> AnswerIndexes = newQuiz.getAnswer(id);

        return newQuiz.addQuestionCompletion(guessedAnswers, requestedQuestion, AnswerIndexes, auth);
    }

    @PostMapping("/actuator/shutdown")
    public ResponseEntity<String> shutdown() {
        return ResponseEntity.ok().body("Successfully accessed");
    }

    @PostMapping("/api/register")
    public ResponseEntity<String> registerUser(@RequestBody User newUser) {
        // Checking if the newUser has valid fields
        if (!newUser.isValid()) {
            return ResponseEntity.status(400).body("Bad Request");
        }

        // Checking if this user email already exists, since it's unique identifier
        if (users.containsEmail(newUser.getEmail())) {
            return ResponseEntity.status(400).body("Bad Request");
        }

        users.register(newUser);

        return ResponseEntity.ok().body("User added successfully");
    }

    @DeleteMapping("/api/quizzes/{id}")
    public ResponseEntity<String> deleteQuestion(@PathVariable("id") int id, Authentication auth) {
        int result = newQuiz.isQuestionOwner(auth, id);

        if (result == 2) {
            return ResponseEntity.status(404).body("Not found");
        } else if (result == 0) {
            return ResponseEntity.status(403).body("Forbidden");
        }

        Question questionToDelete = newQuiz.getById(id);
        newQuiz.deleteQuestion(questionToDelete);

        return ResponseEntity.status(204).body("No content");
    }
}