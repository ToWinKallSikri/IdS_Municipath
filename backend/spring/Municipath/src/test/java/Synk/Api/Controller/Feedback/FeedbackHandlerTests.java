package Synk.Api.Controller.Feedback;

import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import Synk.Api.Controller.Analysis.AnalysisHandler;

@RunWith(SpringRunner.class)
@SpringBootTest
public class FeedbackHandlerTests {

	@Autowired
	AnalysisHandler handler;
	
	@Test
	public void testGiveFeedback() {
		//TODO
	}
	
}
