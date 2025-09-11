import { render, screen } from '@testing-library/react'
import { describe, it, expect } from 'vitest'
import Skeleton from '../Skeleton'

describe('Skeleton', () => {
  it('renders with default props', () => {
    render(<Skeleton />)
    const skeleton = screen.getByTestId('skeleton')
    expect(skeleton).toBeInTheDocument()
    expect(skeleton).toHaveStyle({
      height: '20px',
      width: '100%'
    })
  })

  it('renders with custom dimensions', () => {
    render(<Skeleton height="50px" width="200px" />)
    const skeleton = screen.getByTestId('skeleton')
    expect(skeleton).toHaveStyle({
      height: '50px',
      width: '200px'
    })
  })

  it('applies additional className', () => {
    render(<Skeleton className="custom-class" />)
    const skeleton = screen.getByTestId('skeleton')
    expect(skeleton).toHaveClass('custom-class')
  })
})