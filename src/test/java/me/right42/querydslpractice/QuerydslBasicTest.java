package me.right42.querydslpractice;

import static me.right42.querydslpractice.entity.QMember.*;
import static me.right42.querydslpractice.entity.QTeam.*;
import static org.assertj.core.api.Assertions.*;

import java.util.List;

import javax.persistence.Entity;
import javax.persistence.EntityManager;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import com.querydsl.core.NonUniqueResultException;
import com.querydsl.core.QueryResults;
import com.querydsl.core.Tuple;
import com.querydsl.jpa.impl.JPAQueryFactory;

import me.right42.querydslpractice.entity.Member;
import me.right42.querydslpractice.entity.QMember;
import me.right42.querydslpractice.entity.QTeam;
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


		assertThat(findMember.getUsername()).isEqualTo("member1");
	}

	@Test
	void startQuerydsl(){

		Member findMember = query
			.selectFrom(member)
			.where(member.username.eq("member1"))
			.fetchOne();

		assertThat(findMember).isNotNull();
		assertThat(findMember.getUsername()).isEqualTo("member1");
	}

	@Test
	void search(){

		Member findMember = query
			.selectFrom(member)
			.where(
				member.username.eq("member1"),
				member.age.eq(10).or(member.age.lt(20))
			)
			.fetchOne();

		assertThat(findMember).isNotNull();
		assertThat(findMember.getUsername()).isEqualTo("member1");
	}

	@Test
	void queryResult(){
		// List<Member> fetch = query
		// 	.selectFrom(member)
		// 	.fetch();

		assertThatThrownBy(() ->
			query
			.selectFrom(member)
			.fetchOne()
		)
		.isInstanceOf(NonUniqueResultException.class)
		;

		QueryResults<Member> results = query
			.selectFrom(member)
			.where(member.age.eq(10))
			.fetchResults();

		System.out.println(results.getTotal());
	}


	@Test
	void sort(){
		int age = 100;
		entityManager.persist(new Member("member5", age));
		entityManager.persist(new Member("member6", age));
		entityManager.persist(new Member(null, age));

		List<Member> members = query
			.selectFrom(member)
			.orderBy(member.age.desc(), member.username.asc().nullsLast())
			.fetch();

		Member member5 = members.get(0);
		Member member6 = members.get(1);
		Member memberNull = members.get(2);

		assertThat(member5.getUsername()).isEqualTo("member5");
		assertThat(member6.getUsername()).isEqualTo("member6");
		assertThat(memberNull.getUsername()).isNull();
	}

	@Test
	void paging1(){
		List<Member> members = query
			.selectFrom(member)
			.orderBy(member.age.desc())
			.offset(1)
			.limit(2)
			.fetch();

		assertThat(members.size()).isEqualTo(2);
	}

	@Test
	void paging2(){
		QueryResults<Member> results = query
			.selectFrom(member)
			.orderBy(member.age.desc())
			.offset(1)
			.limit(2)
			.fetchResults();

		assertThat(results.getTotal()).isEqualTo(4);
		assertThat(results.getLimit()).isEqualTo(2);
		assertThat(results.getOffset()).isEqualTo(1);
		assertThat(results.getResults().size()).isEqualTo(2);
	}

	@Test
	void grouping1(){
		List<Tuple> fetch = query
			.select(
				member.count(),
				member.age.max(),
				member.age.min(),
				member.age.avg(),
				member.age.sum()
			)
			.from(member)
			.fetch();

		Tuple tuple = fetch.get(0);

		assertThat(tuple.get(member.count())).isEqualTo(4);
		assertThat(tuple.get(member.age.max())).isEqualTo(40);
		assertThat(tuple.get(member.age.min())).isEqualTo(10);
		assertThat(tuple.get(member.age.avg())).isEqualTo(25);
		assertThat(tuple.get(member.age.sum())).isEqualTo(100);
	}

	/*
		팀별 나이의 평균구하기
	 */
	@Test
	void grouping2(){

		List<Tuple> fetch = query
			.select(
				team.name,
				member.age.avg()
			)
			.from(member)
			.join(member.team, team)
			.groupBy(team.name)
			.fetch();

		Tuple teamA = fetch.get(0);
		Tuple teamB = fetch.get(1);

		assertThat(teamA.get(team.name)).isEqualTo("teamA");
		assertThat(teamA.get(member.age.avg())).isEqualTo(15);

		assertThat(teamB.get(team.name)).isEqualTo("teamB");
		assertThat(teamB.get(member.age.avg())).isEqualTo(35);
	}
}

