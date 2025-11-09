import random
import pandas as pd
import matplotlib.pyplot as plt
from collections import defaultdict

class Traits:
    def __init__(
        self,
        spawn_rate: float,
        death_rate: float,
        replication_rate: float,
        mutation_rate: float,
    ):
        self.spawn_rate = spawn_rate
        self.death_rate = death_rate
        self.replication_rate = replication_rate
        self.mutation_rate = mutation_rate

    def __str__(self):
        return f"Traits(spawn_rate={self.spawn_rate}, death_rate={self.death_rate}, replication_rate={self.replication_rate}, mutation_rate={self.mutation_rate})"


class Individual:
    def __init__(self, family_code: str, traits: Traits):
        self.family_code = family_code
        self.traits = traits
        
    def __str__(self):
        return f"Individual(family_code={self.family_code}, traits={self.traits})"

    def can_die(self) -> bool:
        return random.random() < self.traits.death_rate

    def can_replicate(self, num_replicators: int, crowding_factor: float) -> bool:
        return random.random() < (
            self.traits.replication_rate * (1 - num_replicators / crowding_factor)
        )

    def can_mutate(self) -> bool:
        return random.random() < self.traits.mutation_rate


def random_delta(x: float) -> float:
    delta = random.random() * 0.01 - 0.005
    if x + delta < 0:
        return 0.1
    if x + delta > 1:
        return 0.9
    return x + delta


def mutate(parent: Individual) -> Individual:
    return Individual(
        family_code=random_family_code(),
        traits=Traits(
            0.0,
            random_delta(parent.traits.death_rate),
            random_delta(parent.traits.replication_rate),
            random_delta(parent.traits.mutation_rate),
        ),
    )
    
def random_family_code() -> str:
    letters = 'ABCDEFGHIJKLMNOPQRSTUVWXYZ'
    return ''.join(random.choice(letters) for i in range(3))


spawn_rate = 0.01
death_rate = 0.02
replicate_rate = 0.04
mutation_rate = 0.04
crowding_factor = 1000
universe_age = 15000

INITIAL_REPLICATOR_ID = random_family_code()
members = []
t = 0

plt.ion()
fig, ax = plt.subplots()
family_counts_history = defaultdict(list)
time_points = []

while t < universe_age:
    if random.random() < spawn_rate:
        members.append(
            Individual(
                INITIAL_REPLICATOR_ID,
                Traits(spawn_rate, death_rate, replicate_rate, mutation_rate),
            )
        )

    for member in members[:]:
        if member.can_die():
            members.remove(member)
        elif member.can_replicate(len(members), crowding_factor):
            offspring = (
                mutate(member)
                if member.can_mutate()
                else Individual(member.family_code, member.traits)
            )
            members.append(offspring)

    if t % 100 == 0:
        family_counts = defaultdict(int)
        for member in members:
            family_counts[member.family_code] += 1
        
        for fam, count in family_counts.items():
            family_counts_history[fam].append(count)
        for fam in set(family_counts_history.keys()) - set(family_counts.keys()):
            family_counts_history[fam].append(0)
        time_points.append(t)
        
        for fam in family_counts_history:
            while len(family_counts_history[fam]) < len(time_points):
                family_counts_history[fam].insert(0, 0)
        
        ax.clear()
        
        if family_counts_history:
            latest_counts = {fam: counts[-1] for fam, counts in family_counts_history.items()}
            top_fams = sorted(latest_counts, key=latest_counts.get, reverse=True)[:5]
        else:
            top_fams = []
        for fam, counts in family_counts_history.items():
            if fam in top_fams:
                ax.plot(time_points, counts, label=fam)
            else:
                ax.plot(time_points, counts, color='gray', linewidth=0.5, alpha=0.5)
        ax.set_xlabel("Time")
        ax.set_ylabel("Population")
        ax.set_title("Population of Each Family Over Time")
        if top_fams:
            ax.legend(loc='upper right', fontsize='small', ncol=1)
        plt.pause(0.01)

    t += 1

plt.ioff()
plt.show()

data = []
for member in members:
    data.append(
        {
            "family_code": member.family_code,
            "spawn_rate": member.traits.spawn_rate,
            "death_rate": member.traits.death_rate,
            "replication_rate": member.traits.replication_rate,
            "mutation_rate": member.traits.mutation_rate,
        }
    )
df = pd.DataFrame(data)
df.to_csv("members.csv", index=False)