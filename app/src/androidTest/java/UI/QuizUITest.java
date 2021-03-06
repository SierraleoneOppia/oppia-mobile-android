package UI;



import android.os.Bundle;

import org.digitalcampus.mobile.learning.R;
import org.digitalcampus.mobile.quiz.Quiz;
import org.digitalcampus.oppia.activity.CourseActivity;
import org.digitalcampus.oppia.activity.WelcomeActivity;
import org.digitalcampus.oppia.model.Activity;
import org.digitalcampus.oppia.model.Course;
import org.digitalcampus.oppia.model.Lang;
import org.digitalcampus.oppia.widgets.QuizWidget;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.lang.reflect.Array;
import java.util.ArrayList;

import androidx.fragment.app.testing.FragmentScenario;
import static androidx.fragment.app.testing.FragmentScenario.launchInContainer;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.RootMatchers.withDecorView;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.core.StringStartsWith.startsWith;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.ActivityTestRule;


@RunWith(AndroidJUnit4.class)
public class QuizUITest {

    private static final String SIMPLE_QUIZ_JSON = "quizzes/simple_quiz.json";
    private static final String FIRST_QUESTION_TITLE = "First question";

    private Activity act;
    private Bundle args;

    @Before
    public void setup() throws Exception {
        // Setting up before every test
        act = new Activity();
        String quizContent = Utils.FileUtils.getStringFromFile(
                InstrumentationRegistry.getInstrumentation().getContext(), SIMPLE_QUIZ_JSON);

        ArrayList<Lang> contents = new ArrayList<>();
        contents.add(new Lang("en", quizContent));
        act.setContents(contents);

        args = new Bundle();
        args.putSerializable(Activity.TAG, act);
        args.putSerializable(Course.TAG, new Course(""));
        args.putBoolean(CourseActivity.BASELINE_TAG, false);
    }

    @Test
    public void dontContinueIfQuestionUnaswered() {
        launchInContainer(QuizWidget.class, args, R.style.Oppia_ToolbarTheme, null);

        onView(withId(R.id.question_text))
                .check(matches(withText(startsWith(FIRST_QUESTION_TITLE))));
        onView(withId(R.id.mquiz_next_btn)).perform(click());

        //If we didn't select any option, it should have stayed in the same question
        onView(withId(R.id.question_text))
                .check(matches(withText(startsWith(FIRST_QUESTION_TITLE))));

    }

    @Test
    public void continueIfQuestionAnswered() {
        launchInContainer(QuizWidget.class, args, R.style.Oppia_ToolbarTheme, null);
        onView(withId(R.id.question_text))
                .check(matches(withText(startsWith(FIRST_QUESTION_TITLE))));

        onView(withText("correctanswer")).perform(click());
        onView(withId(R.id.mquiz_next_btn)).perform(click());

        onView(withId(R.id.question_text))
                .check(matches(not(withText(startsWith(FIRST_QUESTION_TITLE)))));

    }
}
