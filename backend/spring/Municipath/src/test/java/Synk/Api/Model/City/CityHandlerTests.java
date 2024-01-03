package Synk.Api.Model.City;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;

import static org.junit.jupiter.api.Assertions.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;


import Synk.Api.Model.MuniciPathMediator;
import Synk.Api.Model.Group.GroupHandler;
import Synk.Api.Model.Post.PointHandler;
import Synk.Api.Model.Post.Position;
import Synk.Api.Model.User.UserHandler;
import jakarta.annotation.PostConstruct;

@RunWith(SpringRunner.class)
@SpringBootTest
public class CityHandlerTests {
	
	@Autowired
    private CityHandler ch;
	@Autowired
    private UserHandler uh;
	@Autowired
    private PointHandler ph;
	@Autowired
    private GroupHandler gh;
	
	@PostConstruct
	public void init() {
		MuniciPathMediator mediator = new MuniciPathMediator();
		ch.setMediator(mediator);
		ph.setMediator(mediator);
		uh.setMediator(mediator);
		gh.setMediator(mediator);
		mediator.setCity(ch);
		mediator.setPoint(ph);
		mediator.setUser(uh);
		mediator.setGroup(gh);
	}
	
	@Test
	void testInitialization() {
		assertDoesNotThrow(() -> new CityHandler());
	}
	
	@Test
	void testCreateDeleteCity() {
		assertFalse(ch.createCity("tokyo", 12345, "naruto", new Position(1, 2)));
		uh.addUser("naruto", "password");
		uh.userValidation("naruto");
		String id = "" + ("tokyo"+12345).hashCode();
		assertFalse(ch.deleteCity(id));
		assertTrue(ch.createCity("tokyo", 12345, "naruto", new Position(1, 2)));
		uh.addUser("sasuke", "password");
		uh.userValidation("sasuke");
		assertFalse(ch.createCity("tokyo", 12345, "sasuke", new Position(1, 2)));
		assertTrue(ch.deleteCity(id));
		uh.removeUser("sasuke");
		uh.removeUser("naruto");
	}
	
	@Test
	void testGetCity() {
		uh.addUser("naruto", "password");
		uh.userValidation("naruto");
		String id = "" + ("tokyo"+12345).hashCode();
		assertTrue(ch.getCity(id) == null);
		ch.createCity("tokyo", 12345, "naruto", new Position(1, 2));
		assertFalse(ch.getCity(id) == null);
		City city = ch.getCity(id);
		assertEquals(ch.getCities().get(0), city);
		assertEquals(ch.getCities("Tokyo").get(0), city);
		ch.deleteCity(id);
		uh.removeUser("naruto");
	}
	
	@Test
	void testUpdateCity() {
		uh.addUser("naruto", "password");
		uh.userValidation("naruto");
		uh.addUser("sasuke", "password");
		uh.userValidation("sasuke");
		String id = "" + ("tokyo"+12345).hashCode();
		Position p1 = new Position(1, 2);
		Position p2 = new Position(2, 1);
		assertFalse(ch.updateCity(id, "berlino", 54321, "sasuke", p2));
		ch.createCity("tokyo", 12345, "naruto", p1);
		City city = ch.getCity(id);
		assertEquals(city.getPos(), p1);
		assertEquals(city.getCap(), 12345);
		assertEquals(city.getName(), "tokyo");
		assertEquals(city.getCurator(), "naruto");
		//ch.deleteCity(id);
		assertTrue(ch.updateCity(id, "berlino", 54321, "sasuke", p2));
		city = ch.getCity(id);
		assertEquals(city.getPos(), p2);
		assertEquals(city.getCap(), 54321);
		assertEquals(city.getName(), "berlino");
		assertEquals(city.getCurator(), "sasuke");
		ch.deleteCity(id);
		uh.removeUser("naruto");
		uh.removeUser("sasuke");
	}
	
}
