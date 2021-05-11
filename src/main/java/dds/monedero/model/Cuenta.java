package dds.monedero.model;

import dds.monedero.exceptions.MaximaCantidadDepositosException;
import dds.monedero.exceptions.MaximoExtraccionDiarioException;
import dds.monedero.exceptions.MontoNegativoException;
import dds.monedero.exceptions.SaldoMenorException;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class Cuenta {

  private double saldo;
  private List<Movimiento> movimientos = new ArrayList<>();
  private double limiteExtraccionPorDia = 1000;
  private int limiteDeDepositosDiarios = 3;

  public Cuenta() {
    saldo = 0;
  }

  public Cuenta(double montoInicial) {
    saldo = montoInicial;
  }

  public void setMovimientos(List<Movimiento> movimientos) {
    this.movimientos = movimientos;
  } // deberían poder agregarse así los movimientos?? no se si es un code smell, pero con el dominio no me cierra

  public void depositar(double cuanto) {
    puedeDepositar(cuanto);
    agregarMovimiento(LocalDate.now(), cuanto, true);
  }

  private void puedeDepositar(double cuanto) {
    esNegativo(cuanto);
    if (getMovimientos().stream().filter(movimiento -> movimiento.fueDepositado(LocalDate.now())).count() >= this.limiteDeDepositosDiarios) {
      throw new MaximaCantidadDepositosException("Ya excedio los " + this.limiteDeDepositosDiarios + " depositos diarios");
    }
  }

  public void extraer(double cuanto) {
    puedeExtraer(cuanto);
    agregarMovimiento(LocalDate.now(), cuanto, false);
  }

  private void puedeExtraer(double cuanto) {
    esNegativo(cuanto);
    double montoExtraidoHoy = getMontoExtraidoA(LocalDate.now());
    double limite = this.limiteExtraccionPorDia - montoExtraidoHoy;
    if (cuanto > limite) {
      throw new MaximoExtraccionDiarioException("No puede extraer mas de $ " + this.limiteExtraccionPorDia
          + " diarios, límite: " + limite);
    }
    if (cuanto > this.getSaldo()) {
      throw new SaldoMenorException("Solo dispone de $" + this.getSaldo());
    }
  }

  private void esNegativo(double cuanto) {
    if (cuanto <= 0) {
      throw new MontoNegativoException(cuanto + ": el monto a ingresar debe ser un valor positivo");
    }
  }

  public void agregarMovimiento(LocalDate fecha, double cuanto, boolean esDeposito) {
    Movimiento movimiento = new Movimiento(fecha, cuanto, esDeposito);
    movimientos.add(movimiento);
    modificarSaldo(movimiento);
  }

  private void modificarSaldo(Movimiento movimiento) {
    if (movimiento.isExtraccion()) {
      this.saldo = this.saldo - movimiento.getMonto();
    } else {
      this.saldo = this.saldo + movimiento.getMonto();
    }
  }

  public double getMontoExtraidoA(LocalDate fecha) {
    return getMovimientos().stream()
        .filter(movimiento -> movimiento.isExtraccion() && movimiento.esDeLaFecha(fecha))
        .mapToDouble(Movimiento::getMonto)
        .sum();
  }

  public List<Movimiento> getMovimientos() {
    return movimientos;
  }

  public double getSaldo() {
    return saldo;
  }

  public void setSaldo(double saldo) {
    this.saldo = saldo;
  }  // no esta bueno tener un método setSaldo, solo debería poder agregarse saldo a través de depositos.
     // volviendo a pensar sobre esto, no creo que sea un code smell, sino que me suena raro pensando en el dominio.

}
