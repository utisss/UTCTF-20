#include "scene.h"

void Scene::render() {
    for(Drawable *thing : *this) {
        thing->render();
    }
}