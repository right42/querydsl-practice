package me.right42.querydslpractice;

import static org.assertj.core.api.Assertions.*;

import javax.persistence.Entity;
import javax.persistence.EntityManager;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import com.querydsl.jpa.impl.JPAQueryFactory;

import me.right42.querydslpractice.entity.Member;
import me.right42.querydslpractice.entity.QMember;
import me.right42.querydslpractice.entity.Team;

@DataJpaTest
class QuerydslBasicTest {

	@Autowired
	EntityManager entityManager;

	JPAQueryFactory query;

	@BeforeEach
	void beforeEach(){
		query = new JPAQueryFactory(entityManager);

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
	}


	@Test
	void startJPQL(){
		String qlString = "select m from Member m where m.username = :username";
		Member findMember = entityManager.createQuery(qlString, Member.class)
			.setParameter("username", "member1")
			.getSingleResult();
		;

		assertThat(findMember.getUsername()).isEqualTo("member1");
	}

	@Test
	void startQuerydsl(){
		QMember member = QMember.member;

		Member findMember = query
			.selectFrom(member)
			.where(member.username.eq("member1"))
			.fetchOne();

		assertThat(findMember.getUsername()).isEqualTo("member1");
	}
}
