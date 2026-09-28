extends Node2D

var player_pos := Vector2(640, 420)
var velocity := Vector2.ZERO
var speed := 260.0
var bullets: Array[Dictionary] = []
var enemies: Array[Dictionary] = [
    {"pos": Vector2(900, 260), "alive": true},
    {"pos": Vector2(1030, 500), "alive": true}
]
var money := 25
var wanted := 0
var health := 100

func _ready() -> void:
    queue_redraw()

func _process(delta: float) -> void:
    var direction := Input.get_vector("move_left", "move_right", "move_up", "move_down")
    velocity = direction * speed
    player_pos += velocity * delta
    player_pos.x = clamp(player_pos.x, 50.0, 1230.0)
    player_pos.y = clamp(player_pos.y, 120.0, 670.0)

    if Input.is_action_just_pressed("fire"):
        var target := get_global_mouse_position()
        var dir := (target - player_pos).normalized()
        bullets.append({"pos": player_pos, "vel": dir * 650.0})

    for bullet in bullets:
        bullet.pos += bullet.vel * delta
    bullets = bullets.filter(func(b): return Rect2(0, 0, 1280, 720).has_point(b.pos))

    for enemy in enemies:
        if not enemy.alive:
            continue
        for bullet in bullets:
            if bullet.pos.distance_to(enemy.pos) < 28.0:
                enemy.alive = false
                money += 10
                wanted = min(wanted + 1, 5)

    queue_redraw()

func _draw() -> void:
    # Sky, desert and road
    draw_rect(Rect2(0, 0, 1280, 720), Color("#d8a85c"))
    draw_rect(Rect2(0, 0, 1280, 170), Color("#8fc5df"))
    draw_circle(Vector2(1080, 90), 45, Color("#f6d67a"))
    draw_rect(Rect2(0, 520, 1280, 200), Color("#b98545"))
    draw_polygon(PackedVector2Array([Vector2(0,520),Vector2(260,300),Vector2(520,520)]), PackedColorArray([Color("#9a6b43")]))
    draw_polygon(PackedVector2Array([Vector2(620,520),Vector2(860,330),Vector2(1100,520)]), PackedColorArray([Color("#9a6b43")]))

    # Town
    draw_rect(Rect2(70, 330, 260, 160), Color("#7b4f32"))
    draw_rect(Rect2(105, 360, 80, 70), Color("#d4b06a"))
    draw_rect(Rect2(215, 350, 85, 80), Color("#c58b52"))
    draw_string(ThemeDB.fallback_font, Vector2(115, 315), "DUSTY CREEK", HORIZONTAL_ALIGNMENT_LEFT, -1, 24, Color.WHITE)

    # Player
    draw_circle(player_pos, 20, Color("#2e2520"))
    draw_circle(player_pos + Vector2(0,-18), 13, Color("#d39a6c"))
    draw_rect(Rect2(player_pos.x-18, player_pos.y-35, 36, 7), Color("#4a2e1d"))
    draw_line(player_pos, get_global_mouse_position(), Color(1,1,1,0.25), 2)

    # Enemies
    for enemy in enemies:
        if enemy.alive:
            draw_circle(enemy.pos, 18, Color("#6d2525"))
            draw_circle(enemy.pos + Vector2(0,-15), 11, Color("#bd7f59"))

    # Bullets
    for bullet in bullets:
        draw_circle(bullet.pos, 5, Color("#f8e7a1"))

    # HUD
    draw_rect(Rect2(20, 20, 310, 82), Color(0,0,0,0.55))
    draw_string(ThemeDB.fallback_font, Vector2(35, 48), "HENRY WESTERN", HORIZONTAL_ALIGNMENT_LEFT, -1, 25, Color.WHITE)
    draw_string(ThemeDB.fallback_font, Vector2(35, 75), "Vida: %d   $%d   Procurado: %d/5" % [health, money, wanted], HORIZONTAL_ALIGNMENT_LEFT, -1, 18, Color.WHITE)
    draw_string(ThemeDB.fallback_font, Vector2(35, 690), "WASD = mover   |   Clique = atirar", HORIZONTAL_ALIGNMENT_LEFT, -1, 18, Color.WHITE)
