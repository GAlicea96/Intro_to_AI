/*
**	Author: Guillermo Alicea
**	UCF Fall 2016 - CAP4630 - Professor Glinos
**	23 OCT 2016
**	Expanded from original one room planner provided
**	as part of assignment 2
** --- IDS IS IMPLEMENTED ---
*/

:- module( planner,
	   [
	       plan/6,planr/6,change_state/3,conditions_met/2,member_state/2,
	       move/3,go/2,test1/0,test2/0,test3/0
	   ]).

:- [utils].

/* Increase depth limit everytime the previous limit could not find
** a solution. [if planr() fails, plan() calls itself]
*/

plan(State, Goal, Been_list, Moves, Depth, State2) :-
				planr(State, Goal, Been_list, Moves, Depth, State2);
				plan(State2, Goal, [State2], [], Depth + 1, State2),!.
planr(State, Goal, _, Moves, _, _) :-
				equal_set(State, Goal),
				write('moves are'), nl,
				reverse_print_stack(Moves).
planr(State, Goal, Been_list, Moves, Depth, State2) :-
				Depth >= 0,
				move(Name, Preconditions, Actions),
				conditions_met(Preconditions, State),
				change_state(State, Actions, Child_state),
				not(member_state(Child_state, Been_list)),
				stack(Child_state, Been_list, New_been_list),
				stack(Name, Moves, New_moves),
				planr(Child_state, Goal, New_been_list, New_moves, Depth - 1, State2),!.

change_state(S, [], S).
change_state(S, [add(P)|T], S_new) :-	change_state(S, T, S2),
					add_to_set(P, S2, S_new), !.
change_state(S, [del(P)|T], S_new) :-	change_state(S, T, S2),
					remove_from_set(P, S2, S_new), !.
conditions_met(P, S) :- subset(P, S).

member_state(S, [H|_]) :-	equal_set(S, H).
member_state(S, [_|T]) :-	member_state(S, T).

/* move types:
** moves have an added room1() or room2() predicate and hand1 or hand2
** which serve to check for blocks being in room1 or room2, respectively,
** and to check if the hand is in room1 or room2, respectively. Each move
** has a set of two - one for a move in room 1 and one for a move in room 2
*/

move(pickup(X), [handempty, clear(X), on(X, Y), room1(X), room1(Y), hand1],
		[del(handempty), del(clear(X)), del(on(X, Y)),
				 add(clear(Y)),	add(holding(X))]).

 move(pickup(X), [handempty, clear(X), on(X, Y), room2(X), room2(Y), hand2],
 		[del(handempty), del(clear(X)), del(on(X, Y)),
 				 add(clear(Y)),	add(holding(X))]).

move(pickup(X), [handempty, clear(X), ontable(X), room1(X), hand1],
		[del(handempty), del(clear(X)), del(ontable(X)),
				 add(holding(X))]).

move(pickup(X), [handempty, clear(X), ontable(X), room2(X), hand2],
		[del(handempty), del(clear(X)), del(ontable(X)),
				 add(holding(X))]).

move(putdown(X), [holding(X), room1(X), hand1],
		[del(holding(X)), add(ontable(X)), add(clear(X)),
				  add(handempty)]).

move(putdown(X), [holding(X), room2(X), hand2],
		[del(holding(X)), add(ontable(X)), add(clear(X)),
				  add(handempty)]).

move(stack(X, Y), [holding(X), clear(Y), room1(X), room1(Y), hand1],
		[del(holding(X)), del(clear(Y)), add(handempty), add(on(X, Y)),
				  add(clear(X))]).

move(stack(X, Y), [holding(X), clear(Y), room2(X), room2(Y), hand2],
		[del(holding(X)), del(clear(Y)), add(handempty), add(on(X, Y)),
				  add(clear(X))]).

/* move between rooms, either while holding or not holding a block,
** and from room1 to room2 or vice versa.
*/

move(goroom1, [handempty, hand2], [add(hand1), del(hand2)]).
move(goroom1, [holding(X), hand2, room2(X)], [add(hand1), add(room1(X)), del(hand2), del(room2(X))]).

move(goroom2, [handempty, hand1], [add(hand2), del(hand1)]).
move(goroom2, [holding(X), hand1, room1(X)], [add(room2(X)), add(hand2), del(hand1), del(room1(X))]).

/* run commands */

go(S, G) :- plan(S, G, [S], [], 0, S).

/* test predicates have added members: hand1 = hand in room1, hand2 = hand in room2,
** room1(X) = X in room1, room2(X) = X in room2. Each block - and the hand - must be explicitly be defined
** to be in room1 or room2.
*/

test1 :- go([hand1, room1(a), room1(b), room1(c), handempty, ontable(b), ontable(c), on(a, b), clear(c), clear(a)],
	          [hand1, room1(a), room1(b), room1(c), handempty, ontable(c), on(b, c), on(a, b), clear(a)]).

test2 :- go([hand1, room1(a), room1(b), room1(c), handempty, ontable(b), ontable(c), on(a, b), clear(a), clear(c)],
				[hand1, room1(a), room1(b), room1(c), handempty, ontable(a), ontable(b), on(c, b), clear(a), clear(c)]).

test3 :- go([hand1, room1(a), room1(b), handempty, ontable(b), on(a, b), clear(a)],
	          [hand1, room2(a), room2(b), handempty, ontable(b), on(a, b), clear(a)]).
