"use strict";

const http = require("node:http");
const { randomUUID } = require("node:crypto");

const PORT = Number(process.env.PORT || 3001);
const HOST = process.env.HOST || "0.0.0.0";

const medicamentos = [
  {
    nombre: "Amoxicilina",
    principioActivo: "Amoxicilina",
    presentacion: "Comprimidos",
    advertencias: "Uso sujeto a indicación veterinaria",
    fuente: "Catálogo demostrativo PetCare"
  },
  {
    nombre: "Carprofeno",
    principioActivo: "Carprofeno",
    presentacion: "Comprimidos",
    advertencias: "No administrar sin evaluación veterinaria",
    fuente: "Catálogo demostrativo PetCare"
  },
  {
    nombre: "Meloxicam",
    principioActivo: "Meloxicam",
    presentacion: "Suspensión oral",
    advertencias: "La dosis depende del peso y diagnóstico",
    fuente: "Catálogo demostrativo PetCare"
  }
];

function responder(response, status, body) {
  response.writeHead(status, {
    "Content-Type": "application/json; charset=utf-8",
    "Access-Control-Allow-Origin": "*",
    "Access-Control-Allow-Headers": "Content-Type",
    "Access-Control-Allow-Methods": "GET,POST,OPTIONS"
  });
  response.end(JSON.stringify(body));
}

function leerJson(request) {
  return new Promise((resolve, reject) => {
    let data = "";
    request.on("data", chunk => {
      data += chunk;
      if (data.length > 1_000_000) {
        reject(new Error("Solicitud demasiado grande"));
        request.destroy();
      }
    });
    request.on("end", () => {
      try {
        resolve(data ? JSON.parse(data) : {});
      } catch (error) {
        reject(new Error("JSON inválido"));
      }
    });
    request.on("error", reject);
  });
}

const server = http.createServer(async (request, response) => {
  if (request.method === "OPTIONS") {
    responder(response, 204, {});
    return;
  }

  const url = new URL(request.url, `http://${request.headers.host || "localhost"}`);

  if (request.method === "GET" && url.pathname === "/api/medicamentos") {
    const nombre = (url.searchParams.get("nombre") || "").trim().toLowerCase();
    if (!nombre) {
      responder(response, 400, { mensaje: "El parámetro nombre es obligatorio" });
      return;
    }
    const resultados = medicamentos.filter(item => item.nombre.toLowerCase().includes(nombre));
    if (resultados.length === 0) {
      responder(response, 404, { mensaje: "No se encontraron medicamentos" });
      return;
    }
    responder(response, 200, resultados);
    return;
  }

  if (request.method === "GET" && url.pathname === "/api/ubicacion-clinica") {
    responder(response, 200, {
      nombre: "Clínica Veterinaria PetCare",
      direccion: "Avenida Central 123, Chillán",
      latitud: -36.6066,
      longitud: -72.1034,
      urlMapa: "https://www.openstreetmap.org/?mlat=-36.6066&mlon=-72.1034#map=17/-36.6066/-72.1034"
    });
    return;
  }

  if (request.method === "POST" && url.pathname === "/api/notificaciones") {
    try {
      const body = await leerJson(request);
      const obligatorios = ["destinatario", "canal", "asunto", "mensaje", "reservaId"];
      const faltantes = obligatorios.filter(campo => body[campo] === undefined || body[campo] === "");
      if (faltantes.length > 0) {
        responder(response, 400, {
          mensaje: "Faltan datos obligatorios",
          campos: faltantes
        });
        return;
      }
      responder(response, 202, {
        idNotificacion: randomUUID(),
        estado: "ACEPTADA"
      });
    } catch (error) {
      responder(response, 400, { mensaje: error.message });
    }
    return;
  }

  responder(response, 404, { mensaje: "Ruta no encontrada" });
});

server.listen(PORT, HOST, () => {
  console.log(`PetCare external API disponible en http://${HOST}:${PORT}`);
});

