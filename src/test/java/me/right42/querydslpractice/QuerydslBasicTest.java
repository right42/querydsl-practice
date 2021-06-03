package me.right42.querydslpractice;

import static com.querydsl.jpa.JPAExpressions.*;
import static me.right42.querydslpractice.entity.QMember.*;
import static me.right42.querydslpractice.entity.QTeam.*;
import static org.assertj.core.api.Assertions.*;

import java.util.List;
import java.util.function.Supplier;

import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceUnit;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.NonUniqueResultException;
import com.querydsl.core.QueryResults;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.ExpressionUtils;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;

import me.right42.querydslpractice.dto.MemberDto;
import me.right42.querydslpractice.dto.QMemberDto;
import me.right42.querydslpractice.dto.UserDto;
import me.right42.querydslpractice.entity.Member;
import me.right42.querydslpractice.entity.QMember;
import me.right42.querydslpractice.entity.QTeam;
import me.right42.querydslpractice.entity.Team;

@DataJpaTest
class QuerydslBasicTest {

	@Autowired
	EntityManager entityManager;

	JPAQueryFactory query;

	@PersistenceUnit
	EntityManagerFactory entityManagerFactory;

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

	/*
		teamA에 속한 모든 회원
	 */
	@Test
	void join(){
		List<Member> members = query
			.selectFrom(member)
			.join(member.team, team)
			.where(team.name.eq("teamA"))
			.fetch();

		assertThat(members)
			.extracting("username")
			.containsExactly("member1", "member2");
	}

	/*
		팀과 이름이 같은 회원 (세타조인)
	 */
	@Test
	void join2(){
		entityManager.persist(new Member("teamA"));
		entityManager.persist(new Member("teamB"));

		List<Member> members = query
			.select(member)
			.from(member, team)
			.where(member.username.eq(team.name))
			.fetch();

		assertThat(members)
			.extracting("username")
			.containsExactly("teamA", "teamB");
	}

	/*
		회원과 팀을 조회하면서, 팀 이름이 teamA인 팀만 조인, 회원은 모두 조회
	 */
	@Test
	void leftJoin(){
		List<Tuple> teamA = query
			.select(member, team)
			.from(member)
			.leftJoin(member.team, team)
				.on(team.name.eq("teamA"))
			.fetch();

		for (Tuple tuple : teamA) {
			System.out.println(tuple);
		}

	}

	/*
		연관관계가 없는 엔티티간의 조인
		팀의 이름과 멤버의 이름의 같은 경우 팀조회
	 */
	@Test
	void join_no_relation(){
		entityManager.persist(new Member("teamA"));
		entityManager.persist(new Member("teamB"));
		entityManager.persist(new Member("teamC"));

		List<Tuple> result = query
			.select(member, team)
			.from(member)
			.leftJoin(team)
				.on(team.name.eq(member.username))
			.fetch();

		for (Tuple tuple : result) {
			System.out.println(tuple);
		}

	}

	@Test
	void fetchJoinNo(){

		Member findMember = query
			.selectFrom(member)
			.where(member.username.eq("member1"))
			.fetchOne();

		boolean loaded = entityManagerFactory.getPersistenceUnitUtil().isLoaded(findMember.getTeam());

		assertThat(loaded).isFalse();
	}

	@Test
	void fetchJoinUse(){
		Member findMember = query
			.selectFrom(member)
			.join(member.team, team).fetchJoin()
			.where(member.username.eq("member1"))
			.fetchOne();

		boolean loaded = entityManagerFactory.getPersistenceUnitUtil().isLoaded(findMember.getTeam());

		assertThat(loaded).isTrue();
	}

	/*
		나이가 가장 많은사람 조회 where 서브쿼리
	 */
	@Test
	void subQuery(){
		QMember memberSub = new QMember("memberSub");

		List<Member> result = query
			.selectFrom(member)
			.where(
				member.age.eq(
					select(memberSub.age.max())
						.from(memberSub)
				)
			)
			.fetch();

		assertThat(result).extracting("age")
			.containsExactly(40);
	}

	/*
		나이가 평균이상인 회원 조회 GOE where
	 */
	@Test
	void subQueryGOE(){
		QMember memberSub = new QMember("memberSub");
		List<Member> result = query
			.selectFrom(member)
			.where(
				member.age.goe(
					select(memberSub.age.avg())
						.from(memberSub)
				)
			)
			.fetch();

		assertThat(result).extracting("age")
			.containsExactly(30, 40);
	}

	/*
		나이가 10 초과인 회원 조회 In where
	 */
	@Test
	void subQueryIn(){
		QMember memberSub = new QMember("memberSub");
		List<Member> result = query
			.selectFrom(member)
			.where(
				member.age.in(
					select(memberSub.age)
					.from(memberSub)
					.where(memberSub.age.gt(10))
				)
			)
			.fetch();

		assertThat(result).extracting("age")
			.containsExactly(20, 30, 40);
	}

	/*
		select 안에 서브쿼리
	 */
	@Test
	void selectSubQuery(){
		QMember memberSub = new QMember("memberSub");
		List<Tuple> result = query
			.select(
				member.username,
				select(memberSub.age.max())
					.from(memberSub)
			)
			.from(member)
			.fetch();

		for (Tuple tuple : result) {
			System.out.println(tuple);
		}

	}

	@Test
	void basicCase(){
		List<String> result = query
			.select(
				member.age
					.when(10).then("10살")
					.when(20).then("20살")
					.otherwise("기타")
			)
			.from(member)
			.fetch();

		for (String s : result) {
			System.out.println("s = " + s);
		}

	}

	@Test
	void constant(){
		List<Tuple> result = query
			.select(member.username, Expressions.constant("A"))
			.from(member)
			.fetch();

		for (Tuple tuple : result) {
			System.out.println(tuple);
		}
	}

	@Test
	void concat(){
		List<String> result = query
			.select(member.username.concat("_").concat(member.age.stringValue()))
			.from(member)
			.where(member.username.eq("member1"))
			.fetch();

		for (String s : result) {
			System.out.println(s);
		}
	}

	@Test
	void findDtoByJPQL(){
		List<MemberDto> result = entityManager
			.createQuery("select new me.right42.querydslpractice.dto.MemberDto(m.username, m.age) from Member m",
				MemberDto.class)
			.getResultList();

		for (MemberDto memberDto : result) {
			System.out.println("memberDto = " + memberDto);
		}
	}

	@Test
	void findDtoBySetter(){
		List<MemberDto> result = query.
			select(Projections.bean(MemberDto.class,
				member.username,
				member.age))
			.from(member)
			.fetch();

		for (MemberDto memberDto : result) {
			System.out.println("memberDto " + memberDto);
		}
	}

	@Test
	void findDtoByField(){
		List<MemberDto> result = query
			.select(
				Projections.fields(MemberDto.class,
					member.username,
					member.age
				)
			)
			.from(member)
			.fetch();

		for (MemberDto memberDto : result) {
			System.out.println("memberDto " + memberDto);
		}
	}

	@Test
	void findDtoByConstructor(){
		List<MemberDto> result = query
			.select(
				Projections.constructor(MemberDto.class,
					member.username,
					member.age
				)
			)
			.from(member)
			.fetch();

		for (MemberDto memberDto : result) {
			System.out.println("memberDto " + memberDto);
		}
	}

	@Test
	void findUserDto(){
		QMember memberSub = new QMember("memberSub");
		List<UserDto> result = query
			.select(
				Projections.fields(UserDto.class,
					member.username.as("name"),

					ExpressionUtils.as(
						select(memberSub.age.max())
							.from(memberSub) ,"age")
				)
			)
			.from(member)
			.fetch();

		for (UserDto userDto : result) {
			System.out.println("userDto " + userDto);
		}
	}

	@Test
	void findDtoByQueryProjection(){
		List<MemberDto> result = query
			.select(new QMemberDto(member.username, member.age))
			.from(member)
			.fetch();

		for (MemberDto memberDto : result) {
			System.out.println(memberDto);
		}
	}

	@Test
	void dynamicQueryBooleanBuilder(){
		String usernameParam = "member1";
		Integer ageParam = 10;

		List<Member> result = searchMember1(usernameParam, ageParam);

		assertThat(result.size()).isEqualTo(1);
	}

	private List<Member> searchMember1(String usernameParam, Integer ageParam) {
		BooleanBuilder builder = new BooleanBuilder();

		if(usernameParam != null) {
			builder.and(member.username.eq(usernameParam));
		}

		if(ageParam != null) {
			builder.and(member.age.eq(ageParam));
		}

		return query
			.selectFrom(member)
			.where(builder)
			.fetch();
	}

	@Test
	void dynamicQueryWhereParam(){
		String usernameParam = "member1";
		Integer ageParam = null;

		List<Member> result = searchMember2(usernameParam, ageParam);

		assertThat(result.size()).isEqualTo(1);
	}

	private List<Member> searchMember2(String usernameParam, Integer ageParam) {
		return query
			.selectFrom(member)
			.where(usernameEqExpression(usernameParam), ageEq(ageParam))
			.fetch();

	}

	private BooleanExpression usernameEqExpression(String username) {
		return member.username.eq(username);
	}

	private BooleanBuilder usernameEq(String usernameParam) {
		return nullSafeBuilder(() -> member.username.eq(usernameParam));
	}

	private BooleanBuilder ageEq(Integer ageParam) {
		return nullSafeBuilder(() -> member.age.eq(ageParam));
	}

	private BooleanBuilder nullSafeBuilder(Supplier<BooleanExpression> expression) {
		try {
			return new BooleanBuilder(expression.get());
		} catch (IllegalArgumentException e) {
			return new BooleanBuilder();
		}
	}
}

