#modeh initiatedAt(carrying(+arg1,+arg3),+time).
#modeh terminatedAt(carrying(+arg1,+arg3),+time).

#modeb happensAt(pass(-arg1,+arg3,+arg1),+time).
#modeb happensAt(pass(+arg1,+arg3,-arg1),+time).
#modeb happensAt(give(+arg1,+arg3,-arg1),+time).
#modeb happensAt(give(-arg1,+arg3,+arg1),+time).
#modeb happensAt(hand(+arg1,+arg3,-arg1),+time).
#modeb happensAt(hand(-arg1,+arg3,+arg1),+time).

#modeb happensAt(get(+arg1,+arg3),+time).
#modeb happensAt(pickup(+arg1,+arg3),+time).
#modeb happensAt(discard(+arg1,+arg3),+time).
#modeb happensAt(putdown(+arg1,+arg3),+time).
#modeb happensAt(leave(+arg1,+arg3),+time).
#modeb happensAt(drop(+arg1,+arg3),+time).
#modeb happensAt(grab(+arg1,+arg3),+time).
#modeb happensAt(take(+arg1,+arg3),+time).