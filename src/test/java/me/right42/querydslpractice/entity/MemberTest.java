package me.right42.querydslpractice.entity;

import static org.assertj.core.api.Assertions.*;

import java.util.List;

import javax.persistence.EntityManager;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

@DataJpaTest
class MemberTest {

	@Autowired
	EntityManager entityManager;

	@Test
	void entityTest(){
		Team teamA = new Team("teamA");
		Team teamB = new Team("teamB");

		entityManager.persist(teamA);
		entityManager.persist(teamB);

		Member member1 = new Member("member1", 10, teamA);
		Member member2 = new Member("member2", 20, teamA);
		Member member3 = new Member("member3", 30, teamB);
		Member member4 = new Member("member4", 40, teamB);

		entityManager.persist(member1);
		entityManager.persist(member2);
		entityManager.persist(member3);
		entityManager.persist(member4);

		entityManager.flush();
		entityManager.clear();

		List<Member> members = entityManager.createQuery("select m from Member m", Member.class)
			.getResultList();

		assertThat(members).isNotNull();
		for (Member member : members) {
			System.out.println("member -> " + member);
			System.out.println("team -> " + member.getTeam());
		}

	}

}
