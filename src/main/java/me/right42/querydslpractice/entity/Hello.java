package me.right42.querydslpractice.entity;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import lombok.Getter;
import lombok.Setter;

@Entity
@Getter @Setter
public class Hello {

	@Id
	@GeneratedValue
	private Long id;
}
