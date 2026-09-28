extends Node2D

# Henry Western - primeira camada de gameplay
# Missão inicial: encontrar o xerife, pegar a missão, localizar o alvo,
# montar no cavalo e retornar para receber a recompensa.

var mission_state := "not_started"
var mission_reward := 50
var npc_pos := Vector2(250, 410)
var horse_pos := Vector2(430, 450)
var target_pos := Vector2(1050, 300)
var mounted := false
var touch_move := Vector2.ZERO
var interaction_message := "Fale com o xerife para começar."

func reset_mission() -> void:
    mission_state = "not_started"
    mounted = false
    interaction_message = "Fale com o xerife para começar."

func interact(player_pos: Vector2) -> void:
    if player_pos.distance_to(npc_pos) < 80.0 and mission_state == "not_started":
        mission_state = "accepted"
        interaction_message = "Missão aceita: encontre o procurado ao norte."
    elif player_pos.distance_to(horse_pos) < 75.0 and mission_state == "accepted":
        mounted = true
        interaction_message = "Você montou no cavalo. Vá até o alvo."
    elif player_pos.distance_to(target_pos) < 90.0 and mission_state == "accepted":
        mission_state = "target_found"
        interaction_message = "Alvo localizado. Volte para Dusty Creek."
    elif player_pos.distance_to(npc_pos) < 80.0 and mission_state == "target_found":
        mission_state = "completed"
        interaction_message = "Missão concluída! Recompensa: $%d" % mission_reward

func get_speed(base_speed: float) -> float:
    return base_speed * (1.65 if mounted else 1.0)

func get_mission_label() -> String:
    match mission_state:
        "not_started": return "MISSÃO: Fale com o xerife"
        "accepted": return "MISSÃO: Encontre o procurado"
        "target_found": return "MISSÃO: Volte para Dusty Creek"
        "completed": return "MISSÃO CONCLUÍDA"
    return ""
